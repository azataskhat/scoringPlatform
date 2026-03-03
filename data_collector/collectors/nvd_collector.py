"""
NVD Collector — сбор данных об уязвимостях IoT-устройств из National Vulnerability Database.
"""

import logging
from datetime import datetime, timedelta, timezone

import aiohttp

from .base import BaseCollector

logger = logging.getLogger(__name__)


class NvdCollector(BaseCollector):
    """Асинхронный сбор CVE из NVD API 2.0."""

    def __init__(self, api_key: str):
        super().__init__(source_name="NVD", api_key=api_key)

    async def collect(self, params: dict) -> list[dict]:
        keywords = params.get("keywords", ["IoT", "MQTT", "Modbus"])
        url = "https://services.nvd.nist.gov/rest/json/cves/2.0"

        # Ищем CVE за последние 30 дней
        pub_end = datetime.now(timezone.utc)
        pub_start = pub_end - timedelta(days=30)

        all_vulns = []
        for keyword in keywords:
            request_params = {
                "keywordSearch": keyword,
                "pubStartDate": pub_start.strftime("%Y-%m-%dT%H:%M:%S.000"),
                "pubEndDate": pub_end.strftime("%Y-%m-%dT%H:%M:%S.000"),
                "resultsPerPage": 20,
            }
            headers = {}
            if self.api_key:
                headers["apiKey"] = self.api_key

            try:
                async with aiohttp.ClientSession() as session:
                    async with session.get(
                        url,
                        params=request_params,
                        headers=headers,
                        timeout=aiohttp.ClientTimeout(total=30),
                    ) as resp:
                        if resp.status != 200:
                            logger.error("[NVD] API returned %d for keyword '%s'", resp.status, keyword)
                            continue
                        data = await resp.json()
            except Exception as e:
                logger.error("[NVD] Request failed for keyword '%s': %s", keyword, e)
                continue

            for item in data.get("vulnerabilities", []):
                cve = item.get("cve", {})
                cve_id = cve.get("id", "")

                metrics = cve.get("metrics", {})
                cvss_data = (
                    metrics.get("cvssMetricV31", [{}])[0].get("cvssData", {})
                    if metrics.get("cvssMetricV31")
                    else metrics.get("cvssMetricV2", [{}])[0].get("cvssData", {})
                    if metrics.get("cvssMetricV2")
                    else {}
                )
                cvss_score = cvss_data.get("baseScore")
                severity = cvss_data.get("baseSeverity", "MEDIUM")

                descriptions = cve.get("descriptions", [])
                desc = next(
                    (d["value"] for d in descriptions if d.get("lang") == "en"),
                    cve_id,
                )

                all_vulns.append({
                    "cveId": cve_id,
                    "severity": severity.upper() if severity else "MEDIUM",
                    "cvssScore": cvss_score,
                    "description": desc[:500],
                })

        logger.info("[NVD] Collected %d vulnerabilities for keywords %s", len(all_vulns), keywords)

        # NVD не возвращает устройства — возвращаем пустой список устройств,
        # уязвимости будут привязаны к устройствам на стороне backend
        # Для интеграции, создаём virtual device
        if all_vulns:
            return [
                self._normalize_device(
                    ip="0.0.0.0",
                    port=0,
                    protocol="N/A",
                    device_type="vulnerability_report",
                    manufacturer="NVD/NIST",
                    city="Global",
                    latitude=None,
                    longitude=None,
                    raw_data={"total_cves": len(all_vulns)},
                    vulnerabilities=all_vulns,
                )
            ]
        return []
