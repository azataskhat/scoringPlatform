"""
Censys OSINT Collector — сбор IoT-устройств через Censys Search API v2.
"""

import logging

import aiohttp

from .base import BaseCollector

logger = logging.getLogger(__name__)


class CensysCollector(BaseCollector):
    """Асинхронный сбор данных из Censys API."""

    def __init__(self, api_id: str, api_secret: str):
        super().__init__(source_name="Censys", api_key=api_id)
        self.api_secret = api_secret

    async def collect(self, params: dict) -> list[dict]:
        query = params.get("query", "location.city: Almaty AND services.port: {80, 443}")
        url = "https://search.censys.io/api/v2/hosts/search"
        headers = {"Accept": "application/json"}

        try:
            auth = aiohttp.BasicAuth(self.api_key, self.api_secret)
            async with aiohttp.ClientSession(auth=auth) as session:
                async with session.get(
                    url,
                    params={"q": query, "per_page": 50},
                    headers=headers,
                    timeout=aiohttp.ClientTimeout(total=30),
                ) as resp:
                    if resp.status != 200:
                        logger.error("[Censys] API returned %d", resp.status)
                        return []
                    data = await resp.json()
        except Exception as e:
            logger.error("[Censys] Request failed: %s", e)
            return []

        devices = []
        for hit in data.get("result", {}).get("hits", []):
            ip = hit.get("ip", "")
            location = hit.get("location", {})
            coords = location.get("coordinates", {})

            for service in hit.get("services", []):
                device = self._normalize_device(
                    ip=ip,
                    port=service.get("port", 0),
                    protocol=service.get("transport_protocol", "TCP").upper(),
                    device_type=self._guess_type(service),
                    manufacturer=hit.get("autonomous_system", {}).get("name"),
                    firmware_version=None,
                    city=location.get("city", "Almaty"),
                    latitude=coords.get("latitude"),
                    longitude=coords.get("longitude"),
                    raw_data={
                        "service_name": service.get("service_name"),
                        "banner": service.get("banner", "")[:500],
                        "as_org": hit.get("autonomous_system", {}).get("name"),
                    },
                )
                devices.append(device)

        return devices

    @staticmethod
    def _guess_type(service: dict) -> str:
        name = (service.get("service_name") or "").lower()
        if "mqtt" in name:
            return "sensor"
        if "http" in name:
            return "router"
        if "modbus" in name:
            return "plc"
        if "rtsp" in name:
            return "camera"
        return "unknown"
