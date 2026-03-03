"""
Базовый класс для OSINT-коллекторов.
"""

import logging
from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import Any

import aiohttp

logger = logging.getLogger(__name__)


class BaseCollector(ABC):
    """Абстрактный коллектор OSINT-данных."""

    def __init__(self, source_name: str, api_key: str):
        self.source_name = source_name
        self.api_key = api_key

    @abstractmethod
    async def collect(self, params: dict) -> list[dict]:
        """Сбор данных из OSINT-источника. Возвращает нормализованный список."""
        ...

    async def send_to_backend(self, data: list[dict], backend_url: str) -> int:
        """Отправляет собранные данные в backend через POST /api/ingest."""
        if not data:
            logger.info("[%s] No data to send", self.source_name)
            return 0

        payload = {
            "source": self.source_name,
            "collectedAt": datetime.now(timezone.utc).isoformat(),
            "data": data,
        }

        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    f"{backend_url}/api/ingest",
                    json=payload,
                    timeout=aiohttp.ClientTimeout(total=30),
                ) as resp:
                    if resp.status == 200:
                        body = await resp.json()
                        logger.info(
                            "[%s] Ingested: %d devices, %d vulns",
                            self.source_name,
                            body.get("devicesIngested", 0),
                            body.get("vulnerabilitiesIngested", 0),
                        )
                    else:
                        text = await resp.text()
                        logger.error("[%s] Backend error %d: %s", self.source_name, resp.status, text)
                    return resp.status
        except Exception as e:
            logger.error("[%s] Failed to send to backend: %s", self.source_name, e)
            return -1

    async def run(self, params: dict, backend_url: str) -> None:
        """Полный цикл: сбор → отправка."""
        logger.info("[%s] Starting collection...", self.source_name)
        try:
            data = await self.collect(params)
            logger.info("[%s] Collected %d records", self.source_name, len(data))
            await self.send_to_backend(data, backend_url)
        except Exception as e:
            logger.error("[%s] Collection failed: %s", self.source_name, e)

    @staticmethod
    def _normalize_device(
        ip: str,
        port: int,
        protocol: str = None,
        device_type: str = None,
        manufacturer: str = None,
        firmware_version: str = None,
        city: str = "Almaty",
        latitude: float = None,
        longitude: float = None,
        raw_data: Any = None,
        vulnerabilities: list[dict] = None,
    ) -> dict:
        """Формирует нормализованную запись устройства."""
        return {
            "ipAddress": ip,
            "port": port,
            "protocol": protocol,
            "deviceType": device_type,
            "manufacturer": manufacturer,
            "firmwareVersion": firmware_version,
            "city": city,
            "latitude": latitude,
            "longitude": longitude,
            "rawData": raw_data,
            "vulnerabilities": vulnerabilities or [],
        }
