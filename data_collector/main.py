"""
OSINT IoT Security Data Collector — точка входа.

Запуск:
    python main.py

Переменные окружения:
    SHODAN_API_KEY, CENSYS_API_ID, CENSYS_API_SECRET,
    GREYNOISE_API_KEY, NVD_API_KEY
"""

import asyncio
import logging
import sys

from scheduler import load_config, build_scheduler

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger(__name__)


async def main():
    logger.info("=" * 60)
    logger.info("  OSINT IoT Security Data Collector")
    logger.info("=" * 60)

    config = load_config()

    scheduler = build_scheduler(config)
    scheduler.start()

    logger.info("Scheduler started. Press Ctrl+C to stop.")

    try:
        while True:
            await asyncio.sleep(1)
    except (KeyboardInterrupt, SystemExit):
        logger.info("Shutting down...")
        scheduler.shutdown()


if __name__ == "__main__":
    asyncio.run(main())
