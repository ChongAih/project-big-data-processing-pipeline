import os
import logging
from datetime import datetime as dt


class MyLogger(object):
    def __init__(self, name: str, log_level: str = None, logfile: bool = False, logfile_path: str = None):
        self._logger = None
        if logfile:
            if not logfile_path:
                logfile_path = "/".join([os.getcwd(), "log", f"{name}_file_{dt.now().strftime('%Y-%m-%d')}.log"])
        if not log_level:
            log_level = logging.INFO
        self._initialize_logger(name, logfile, logfile_path, log_level)

    def _initialize_logger(self, name: str, logfile: bool, logfile_path: str, log_level: str):
        # Create console handlers
        c_handler = logging.StreamHandler()
        c_format = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
        c_handler.setFormatter(c_format)

        if logfile:
            # Create file handlers
            f_handler = logging.FileHandler(logfile_path)
            f_format = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
            f_handler.setFormatter(f_format)

        # Create a custom logger and add handlers to the logger
        self._logger = logging.getLogger(name)
        self._logger.addHandler(c_handler)
        if logfile:
            self._logger.addHandler(f_handler)

        # Set handlers level
        self._logger.setLevel(log_level)

    def debug(self, message: str, *args, **kwargs):
        self._logger.debug(message, *args, **kwargs)

    def info(self, message: str, *args, **kwargs):
        self._logger.info(message, *args, **kwargs)

    def warning(self, message: str, *args, **kwargs):
        self._logger.warning(message, *args, **kwargs)

    def error(self, message: str, *args, **kwargs):
        self._logger.error(message, *args, **kwargs)

    def critical(self, message: str, *args, **kwargs):
        self._logger.critical(message, *args, **kwargs)


if __name__ == "__main__":
    logger = MyLogger("logger")
    logger.debug('This is a debug message')
    logger.info('This is an info message')
    logger.warning('This is a warning message')
    logger.error('This is an error message', exc_info=True)
    logger.critical('This is a critical message')
