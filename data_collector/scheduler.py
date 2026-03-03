"""
APScheduler оркестрация — запуск коллекторов и скоринга по расписанию.
"""

import asyncio
import logging
import os

import yaml
from apscheduler.schedulers.asyncio import AsyncIOScheduler

from collectors import ShodanCollector, CensysCollector, GreyNoiseCollector, NvdCollector
from scoring.scorer import Scorer

logger = logging.getLogger(__name__)


def load_config(path: str = "config.yaml") -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def build_scheduler(config: dict) -> AsyncIOScheduler:
    """Создаёт и настраивает APScheduler."""
    scheduler = AsyncIOScheduler()
    backend_url = config["backend"]["url"]
    scorer = Scorer(backend_url)
    collectors_cfg = config.get("collectors", {})

    # --- Shodan ---
    if collectors_cfg.get("shodan", {}).get("enabled"):
        api_key = os.environ.get(collectors_cfg["shodan"]["api_key_env"], "")
        if api_key:
            collector = ShodanCollector(api_key)
            interval = collectors_cfg["shodan"].get("interval_minutes", 60)
            params = {"query": collectors_cfg["shodan"].get("query", "")}

            async def run_shodan():
                await collector.run(params, backend_url)

            scheduler.add_job(run_shodan, "interval", minutes=interval, id="shodan",
                              name="Shodan Collector", replace_existing=True)
            logger.info("Shodan collector scheduled every %d min", interval)
        else:
            logger.warning("SHODAN_API_KEY not set — Shodan collector disabled")

    # --- Censys ---
    if collectors_cfg.get("censys", {}).get("enabled"):
        api_id = os.environ.get(collectors_cfg["censys"]["api_key_env"], "")
        api_secret = os.environ.get(collectors_cfg["censys"].get("api_secret_env", ""), "")
        if api_id and api_secret:
            collector = CensysCollector(api_id, api_secret)
            interval = collectors_cfg["censys"].get("interval_minutes", 120)
            params = {"query": collectors_cfg["censys"].get("query", "")}

            async def run_censys():
                await collector.run(params, backend_url)

            scheduler.add_job(run_censys, "interval", minutes=interval, id="censys",
                              name="Censys Collector", replace_existing=True)
            logger.info("Censys collector scheduled every %d min", interval)
        else:
            logger.warning("Censys API credentials not set — Censys collector disabled")

    # --- GreyNoise ---
    if collectors_cfg.get("greynoise", {}).get("enabled"):
        api_key = os.environ.get(collectors_cfg["greynoise"]["api_key_env"], "")
        if api_key:
            collector = GreyNoiseCollector(api_key)
            interval = collectors_cfg["greynoise"].get("interval_minutes", 90)
            params = {"query": collectors_cfg["greynoise"].get("query", "")}

            async def run_greynoise():
                await collector.run(params, backend_url)

            scheduler.add_job(run_greynoise, "interval", minutes=interval, id="greynoise",
                              name="GreyNoise Collector", replace_existing=True)
            logger.info("GreyNoise collector scheduled every %d min", interval)
        else:
            logger.warning("GREYNOISE_API_KEY not set — GreyNoise collector disabled")

    # --- NVD ---
    if collectors_cfg.get("nvd", {}).get("enabled"):
        api_key = os.environ.get(collectors_cfg["nvd"]["api_key_env"], "")
        collector = NvdCollector(api_key)
        interval = collectors_cfg["nvd"].get("interval_minutes", 360)
        params = {"keywords": collectors_cfg["nvd"].get("keywords", ["IoT"])}

        async def run_nvd():
            await collector.run(params, backend_url)

        scheduler.add_job(run_nvd, "interval", minutes=interval, id="nvd",
                          name="NVD Collector", replace_existing=True)
        logger.info("NVD collector scheduled every %d min", interval)

    # --- Scoring recalculation ---
    scoring_interval = config.get("scoring", {}).get("interval_minutes", 30)

    async def run_scoring():
        await scorer.trigger_scoring()

    scheduler.add_job(run_scoring, "interval", minutes=scoring_interval, id="scoring",
                      name="Scoring Recalculation", replace_existing=True)
    logger.info("Scoring recalculation scheduled every %d min", scoring_interval)

    return scheduler
