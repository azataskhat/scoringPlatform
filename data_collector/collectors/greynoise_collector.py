"""
GreyNoise OSINT Collector — сбор данных о шумящих IoT-устройствах.
"""

import logging

import aiohttp

from .base import BaseCollector

logger = logging.getLogger(__name__)


class GreyNoiseCollector(BaseCollector):
    """Асинхронный сбор данных из GreyNoise API."""

    def __init__(self, api_key: str):
        super().__init__(source_name="GreyNoise", api_key=api_key)

    async def collect(self, params: dict) -> list[dict]:
        query = params.get("query", "city:Almaty")
        url = "https://api.greynoise.io/v3/community"
        headers = {"key": self.api_key, "Accept": "application/json"}

        # GreyNoise Community API works per-IP, so we use GNQL for enterprise
        # Fallback: use experimental endpoint
        gnql_url = "https://api.greynoise.io/v3/gnql"
        request_params = {"query": query, "size": 50}

        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    gnql_url,
                    params=request_params,
                    headers=headers,
                    timeout=aiohttp.ClientTimeout(total=30),
                ) as resp:
                    if resp.status != 200:
                        logger.error("[GreyNoise] API returned %d", resp.status)
                        return []
                    data = await resp.json()
        except Exception as e:
            logger.error("[GreyNoise] Request failed: %s", e)
            return []

        devices = []
        for hit in data.get("data", []):
            metadata = hit.get("metadata", {})
            device = self._normalize_device(
                ip=hit.get("ip", ""),
                port=hit.get("port", 0),
                protocol=metadata.get("protocol", "TCP").upper(),
                device_type=metadata.get("device_type", "unknown"),
                manufacturer=metadata.get("manufacturer"),
                firmware_version=metadata.get("os_version"),
                city=metadata.get("city", "Almaty"),
                latitude=metadata.get("latitude"),
                longitude=metadata.get("longitude"),
                raw_data={
                    "classification": hit.get("classification"),
                    "noise": hit.get("noise"),
                    "tags": hit.get("tags", []),
                    "last_seen": hit.get("last_seen"),
                },
            )
            devices.append(device)

        return devices
