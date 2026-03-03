"""
Shodan OSINT Collector — сбор IoT-устройств через Shodan API.
"""

import logging

import aiohttp

from .base import BaseCollector

logger = logging.getLogger(__name__)

# Mapping Shodan product names to device types
DEVICE_TYPE_MAP = {
    "camera": "camera",
    "webcam": "camera",
    "hikvision": "camera",
    "dahua": "camera",
    "router": "router",
    "mikrotik": "router",
    "routeros": "router",
    "mqtt": "sensor",
    "mosquitto": "sensor",
    "modbus": "plc",
    "siemens": "plc",
    "bacnet": "building_automation",
    "coap": "sensor",
}


def _guess_device_type(banner: str, product: str | None) -> str:
    """Определяет тип устройства по баннеру/продукту."""
    text = f"{banner} {product or ''}".lower()
    for keyword, dtype in DEVICE_TYPE_MAP.items():
        if keyword in text:
            return dtype
    return "unknown"


class ShodanCollector(BaseCollector):
    """Асинхронный сбор данных из Shodan Search API."""

    def __init__(self, api_key: str):
        super().__init__(source_name="Shodan", api_key=api_key)

    async def collect(self, params: dict) -> list[dict]:
        query = params.get("query", "city:Almaty port:80,443,1883")
        url = "https://api.shodan.io/shodan/host/search"
        request_params = {"key": self.api_key, "query": query, "page": 1}

        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(url, params=request_params, timeout=aiohttp.ClientTimeout(total=30)) as resp:
                    if resp.status != 200:
                        logger.error("[Shodan] API returned %d", resp.status)
                        return []
                    data = await resp.json()
        except Exception as e:
            logger.error("[Shodan] Request failed: %s", e)
            return []

        devices = []
        for match in data.get("matches", []):
            location = match.get("location", {})
            banner = match.get("data", "")
            product = match.get("product")

            vulns = []
            for cve_id in match.get("vulns", {}).keys():
                vuln_info = match["vulns"][cve_id]
                cvss = None
                if isinstance(vuln_info, dict):
                    cvss = vuln_info.get("cvss")
                vulns.append({
                    "cveId": cve_id,
                    "severity": _cvss_to_severity(cvss),
                    "cvssScore": cvss,
                    "description": f"Vulnerability {cve_id} detected by Shodan",
                })

            device = self._normalize_device(
                ip=match.get("ip_str", ""),
                port=match.get("port", 0),
                protocol=match.get("transport", "tcp").upper(),
                device_type=_guess_device_type(banner, product),
                manufacturer=match.get("org"),
                firmware_version=match.get("version"),
                city=location.get("city", "Almaty"),
                latitude=location.get("latitude"),
                longitude=location.get("longitude"),
                raw_data={"banner": banner[:500], "product": product, "os": match.get("os")},
                vulnerabilities=vulns,
            )
            devices.append(device)

        return devices


def _cvss_to_severity(cvss: float | None) -> str:
    if cvss is None:
        return "MEDIUM"
    if cvss >= 9.0:
        return "CRITICAL"
    if cvss >= 7.0:
        return "HIGH"
    if cvss >= 4.0:
        return "MEDIUM"
    return "LOW"
