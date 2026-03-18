"""
Scorer — вычисление параметров r1..a2 на стороне коллектора
и отправка результатов в backend для пересчёта итогового скоринга.
"""

import logging
import os
import time

import aiohttp
import numpy as np

logger = logging.getLogger(__name__)


class Scorer:
    """Вычисляет метрики качества данных и запускает пересчёт скоринга в backend."""

    def __init__(self, backend_url: str):
        self.backend_url = backend_url
        self.api_key = os.environ.get("API_KEY", "")
        self._api_timings: dict[str, list[float]] = {}  # source_name -> [response_times]

    def record_api_timing(self, source_name: str, elapsed_seconds: float) -> None:
        """Записывает время ответа API для расчёта a2."""
        self._api_timings.setdefault(source_name, [])
        self._api_timings[source_name].append(elapsed_seconds)
        # Храним только последние 100 замеров
        if len(self._api_timings[source_name]) > 100:
            self._api_timings[source_name] = self._api_timings[source_name][-100:]

    def compute_a2(self, source_name: str) -> float:
        """Нормализованное время ответа API (чем быстрее, тем ближе к 1)."""
        timings = self._api_timings.get(source_name, [])
        if not timings:
            return 0.5
        avg = np.mean(timings)
        # Нормализуем: 0 сек → 1.0, 10+ сек → 0.0
        return max(0.0, min(1.0, 1.0 - avg / 10.0))

    @staticmethod
    def compute_field_completeness(records: list[dict]) -> float:
        """c1 — доля заполненных полей."""
        if not records:
            return 0.0
        fields = ["ipAddress", "port", "protocol", "deviceType", "manufacturer",
                  "firmwareVersion", "city", "latitude", "longitude"]
        total_fields = len(fields) * len(records)
        filled = sum(
            1 for rec in records for f in fields
            if rec.get(f) is not None and rec.get(f) != ""
        )
        return filled / total_fields if total_fields > 0 else 0.0

    @staticmethod
    def compute_device_type_coverage(records: list[dict]) -> float:
        """c2 — покрытие типов устройств."""
        known_types = {"camera", "router", "sensor", "plc", "gateway", "server"}
        found = {r.get("deviceType") for r in records if r.get("deviceType")}
        return len(found & known_types) / len(known_types) if known_types else 0.0

    @staticmethod
    def compute_data_accuracy(records: list[dict]) -> float:
        """r1 — точность (доля записей с валидным IP и портом)."""
        if not records:
            return 0.0
        valid = sum(
            1 for r in records
            if r.get("ipAddress") and r.get("port") and int(r.get("port", 0)) > 0
        )
        return valid / len(records)

    async def trigger_scoring(self) -> None:
        """Запускает пересчёт скоринга для всех источников на backend."""
        headers = {}
        if self.api_key:
            headers["X-API-Key"] = self.api_key

        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    f"{self.backend_url}/api/scoring/run",
                    headers=headers,
                    timeout=aiohttp.ClientTimeout(total=60),
                ) as resp:
                    if resp.status == 200:
                        logger.info("Scoring recalculation triggered successfully")
                    else:
                        text = await resp.text()
                        logger.error("Scoring trigger failed (%d): %s", resp.status, text)
        except Exception as e:
            logger.error("Failed to trigger scoring: %s", e)
