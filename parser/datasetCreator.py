import argparse
import csv
import json
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


API_URL = "https://api.coinlore.net/api/ticker/"
YAHOO_CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart"


@dataclass
class CollectorConfig:
	source: str
	coin_id: int
	interval_sec: int
	samples: int
	output: Path
	candles_output: Optional[Path]
	candle_sec: int
	yahoo_symbol: str
	yahoo_interval: str
	yahoo_range: str
	start_date: Optional[str]
	end_date: Optional[str]
	windows_output: Optional[Path]
	lookback: int
	horizon: int
	timeout: int
	retries: int


def fetch_ticker(coin_id: int, timeout: int, retries: int) -> Dict[str, str]:
	params = urlencode({"id": coin_id})
	url = f"{API_URL}?{params}"
	req = Request(url, headers={"User-Agent": "AI-Trader-DatasetCreator/1.0"})

	last_error: Optional[Exception] = None
	for attempt in range(1, retries + 1):
		try:
			with urlopen(req, timeout=timeout) as response:
				payload = json.loads(response.read().decode("utf-8"))
				if not payload:
					raise ValueError("Coinlore returned empty payload")
				return payload[0]
		except (HTTPError, URLError, TimeoutError, ValueError, json.JSONDecodeError) as err:
			last_error = err
			if attempt < retries:
				sleep_for = min(2 ** (attempt - 1), 10)
				print(f"[WARN] request failed ({attempt}/{retries}), retry in {sleep_for}s: {err}")
				time.sleep(sleep_for)

	raise RuntimeError(f"Cannot fetch ticker after {retries} attempts: {last_error}")


def parse_iso_date_to_epoch(date_str: str, is_end: bool) -> int:
	base = datetime.strptime(date_str, "%Y-%m-%d").replace(tzinfo=timezone.utc)
	if is_end:
		# Yahoo period2 is exclusive, add one day to include end date.
		base = base + timedelta(days=1)
	return int(base.timestamp())


def fetch_yahoo_history(
	symbol: str,
	interval: str,
	range_value: str,
	start_date: Optional[str],
	end_date: Optional[str],
	timeout: int,
	retries: int,
) -> List[Dict[str, object]]:
	params = {
		"interval": interval,
		"includePrePost": "false",
		"events": "history",
	}

	if start_date and end_date:
		params["period1"] = str(parse_iso_date_to_epoch(start_date, is_end=False))
		params["period2"] = str(parse_iso_date_to_epoch(end_date, is_end=True))
	else:
		params["range"] = range_value

	url = f"{YAHOO_CHART_URL}/{symbol}?{urlencode(params)}"
	req = Request(url, headers={"User-Agent": "AI-Trader-DatasetCreator/1.0"})

	last_error: Optional[Exception] = None
	for attempt in range(1, retries + 1):
		try:
			with urlopen(req, timeout=timeout) as response:
				payload = json.loads(response.read().decode("utf-8"))
				chart = payload.get("chart", {})
				error_obj = chart.get("error")
				if error_obj:
					raise RuntimeError(f"Yahoo API error: {error_obj}")

				result = chart.get("result")
				if not result:
					raise ValueError("Yahoo returned empty result")

				first = result[0]
				timestamps = first.get("timestamp") or []
				indicators = first.get("indicators", {})
				quote_arr = indicators.get("quote") or []
				if not quote_arr:
					raise ValueError("Yahoo quote data is missing")

				quote = quote_arr[0]
				opens = quote.get("open") or []
				highs = quote.get("high") or []
				lows = quote.get("low") or []
				closes = quote.get("close") or []
				volumes = quote.get("volume") or []

				rows: List[Dict[str, object]] = []
				for i, ts in enumerate(timestamps):
					if i >= len(closes):
						break

					close_price = closes[i]
					if close_price is None:
						continue

					dt = datetime.fromtimestamp(int(ts), tz=timezone.utc)
					rows.append(
						{
							"timestamp_utc": dt.isoformat().replace("+00:00", "Z"),
							"symbol": symbol,
							"interval": interval,
							"open": opens[i] if i < len(opens) else None,
							"high": highs[i] if i < len(highs) else None,
							"low": lows[i] if i < len(lows) else None,
							"close": close_price,
							"volume": volumes[i] if i < len(volumes) else None,
							"source": "yahoo_finance_chart",
						}
					)

				if not rows:
					raise ValueError("Yahoo returned no usable candles")

				return rows
		except (HTTPError, URLError, TimeoutError, ValueError, RuntimeError, json.JSONDecodeError) as err:
			last_error = err
			if attempt < retries:
				sleep_for = min(2 ** (attempt - 1), 10)
				print(f"[WARN] request failed ({attempt}/{retries}), retry in {sleep_for}s: {err}")
				time.sleep(sleep_for)

	raise RuntimeError(f"Cannot fetch Yahoo history after {retries} attempts: {last_error}")


def safe_float(value: Optional[str]) -> Optional[float]:
	if value is None or value == "":
		return None
	try:
		return float(value)
	except (TypeError, ValueError):
		return None


def normalize_row(api_row: Dict[str, str], fetched_at_utc: datetime) -> Dict[str, object]:
	ts = fetched_at_utc.isoformat().replace("+00:00", "Z")
	return {
		"fetched_at_utc": ts,
		"coin_id": int(api_row.get("id", 0)),
		"symbol": api_row.get("symbol", ""),
		"name": api_row.get("name", ""),
		"rank": int(api_row.get("rank", 0)),
		"price_usd": safe_float(api_row.get("price_usd")),
		"price_btc": safe_float(api_row.get("price_btc")),
		"volume24": safe_float(api_row.get("volume24")),
		"market_cap_usd": safe_float(api_row.get("market_cap_usd")),
		"percent_change_1h": safe_float(api_row.get("percent_change_1h")),
		"percent_change_24h": safe_float(api_row.get("percent_change_24h")),
		"percent_change_7d": safe_float(api_row.get("percent_change_7d")),
		"ts_api": int(api_row.get("ts", 0)),
		"source": "coinlore_ticker",
	}


def append_rows_to_csv(path: Path, rows: Iterable[Dict[str, object]], fieldnames: List[str]) -> int:
	path.parent.mkdir(parents=True, exist_ok=True)

	row_count = 0
	write_header = not path.exists() or path.stat().st_size == 0
	with path.open("a", newline="", encoding="utf-8") as f:
		writer = csv.DictWriter(f, fieldnames=fieldnames)
		if write_header:
			writer.writeheader()
		for row in rows:
			writer.writerow(row)
			row_count += 1

	return row_count


def write_rows_to_csv(path: Path, rows: Iterable[Dict[str, object]], fieldnames: List[str]) -> int:
	path.parent.mkdir(parents=True, exist_ok=True)

	row_count = 0
	with path.open("w", newline="", encoding="utf-8") as f:
		writer = csv.DictWriter(f, fieldnames=fieldnames)
		writer.writeheader()
		for row in rows:
			writer.writerow(row)
			row_count += 1

	return row_count


def read_rows(path: Path) -> List[Dict[str, str]]:
	if not path.exists() or path.stat().st_size == 0:
		return []

	with path.open("r", newline="", encoding="utf-8") as f:
		return list(csv.DictReader(f))


def parse_utc_iso(ts: str) -> datetime:
	# Convert "...Z" timestamps from CSV back to timezone-aware UTC datetime.
	return datetime.fromisoformat(ts.replace("Z", "+00:00"))


def floor_to_bucket(dt: datetime, bucket_sec: int) -> datetime:
	epoch = int(dt.timestamp())
	floored = (epoch // bucket_sec) * bucket_sec
	return datetime.fromtimestamp(floored, tz=timezone.utc)


def build_candles(rows: List[Dict[str, str]], candle_sec: int) -> List[Dict[str, object]]:
	if candle_sec <= 0:
		return []

	buckets: Dict[Tuple[int, int], List[Tuple[datetime, float]]] = {}
	for row in rows:
		price = safe_float(row.get("price_usd"))
		ts = row.get("fetched_at_utc", "")
		if price is None or not ts:
			continue

		dt = parse_utc_iso(ts)
		bucket_start = floor_to_bucket(dt, candle_sec)
		key = (int(row.get("coin_id", "0")), int(bucket_start.timestamp()))
		buckets.setdefault(key, []).append((dt, price))

	candles: List[Dict[str, object]] = []
	for (coin_id, bucket_ts), points in sorted(buckets.items(), key=lambda x: x[0][1]):
		points.sort(key=lambda x: x[0])
		prices = [p[1] for p in points]
		bucket_start = datetime.fromtimestamp(bucket_ts, tz=timezone.utc)
		bucket_end = bucket_start.timestamp() + candle_sec

		candles.append(
			{
				"coin_id": coin_id,
				"interval_sec": candle_sec,
				"candle_start_utc": bucket_start.isoformat().replace("+00:00", "Z"),
				"candle_end_utc": datetime.fromtimestamp(int(bucket_end), tz=timezone.utc)
				.isoformat()
				.replace("+00:00", "Z"),
				"open": prices[0],
				"high": max(prices),
				"low": min(prices),
				"close": prices[-1],
				"tick_count": len(prices),
			}
		)

	return candles


def save_candles_csv(path: Path, candles: List[Dict[str, object]]) -> int:
	if not candles:
		return 0

	fieldnames = [
		"coin_id",
		"interval_sec",
		"candle_start_utc",
		"candle_end_utc",
		"open",
		"high",
		"low",
		"close",
		"tick_count",
	]

	path.parent.mkdir(parents=True, exist_ok=True)
	with path.open("w", newline="", encoding="utf-8") as f:
		writer = csv.DictWriter(f, fieldnames=fieldnames)
		writer.writeheader()
		writer.writerows(candles)

	return len(candles)


def build_windows(
	rows: List[Dict[str, str]],
	lookback: int,
	horizon: int,
	price_key: str,
	ts_key: str,
) -> List[Dict[str, object]]:
	prices: List[Optional[float]] = [safe_float(r.get(price_key)) for r in rows]
	ts_values: List[str] = [r.get(ts_key, "") for r in rows]

	windows: List[Dict[str, object]] = []
	max_start = len(rows) - lookback - horizon + 1
	if max_start <= 0:
		return windows

	for start in range(max_start):
		hist = prices[start : start + lookback]
		future = prices[start + lookback : start + lookback + horizon]
		if any(v is None for v in hist) or any(v is None for v in future):
			continue

		hist_vals = [float(v) for v in hist if v is not None]
		future_vals = [float(v) for v in future if v is not None]
		last_hist = hist_vals[-1]
		target_price = future_vals[-1]
		target_return = (target_price - last_hist) / last_hist if last_hist else 0.0

		row = {
			"window_start": ts_values[start],
			"window_end": ts_values[start + lookback - 1],
			"target_time": ts_values[start + lookback + horizon - 1],
			"target_price": target_price,
			"target_return": target_return,
		}
		for idx, value in enumerate(hist_vals, start=1):
			row[f"lag_{idx}"] = value
		windows.append(row)

	return windows


def save_windows_csv(path: Path, windows: List[Dict[str, object]], lookback: int) -> int:
	if not windows:
		return 0

	fieldnames = ["window_start", "window_end", "target_time"]
	fieldnames.extend(f"lag_{i}" for i in range(1, lookback + 1))
	fieldnames.extend(["target_price", "target_return"])

	path.parent.mkdir(parents=True, exist_ok=True)
	with path.open("w", newline="", encoding="utf-8") as f:
		writer = csv.DictWriter(f, fieldnames=fieldnames)
		writer.writeheader()
		writer.writerows(windows)

	return len(windows)


def collect_timeseries(config: CollectorConfig) -> List[Dict[str, object]]:
	collected: List[Dict[str, object]] = []

	print(
		f"[INFO] Start collecting coin_id={config.coin_id}, interval={config.interval_sec}s, samples={config.samples}"
	)

	for i in range(config.samples):
		fetched_at = datetime.now(timezone.utc)
		ticker = fetch_ticker(config.coin_id, timeout=config.timeout, retries=config.retries)
		row = normalize_row(ticker, fetched_at)
		collected.append(row)

		price = row.get("price_usd")
		print(f"[INFO] sample {i + 1}/{config.samples}, price_usd={price}, at={row['fetched_at_utc']}")

		if i < config.samples - 1:
			time.sleep(config.interval_sec)

	return collected


def parse_args() -> CollectorConfig:
	parser = argparse.ArgumentParser(
		description="Collect time-series datasets from Coinlore or Yahoo Finance"
	)
	parser.add_argument(
		"--source",
		choices=["coinlore", "yahoo"],
		default="coinlore",
		help="Data source: coinlore (realtime polling) or yahoo (historical candles)",
	)
	parser.add_argument("--coin-id", type=int, default=90, help="Coinlore coin id (e.g. 90 for BTC)")
	parser.add_argument("--interval-sec", type=int, default=60, help="Polling interval in seconds")
	parser.add_argument("--samples", type=int, default=120, help="How many points to collect")
	parser.add_argument(
		"--output",
		type=Path,
		default=Path("data") / "coinlore_timeseries.csv",
		help="Path to raw time-series CSV",
	)
	parser.add_argument(
		"--candles-output",
		type=Path,
		default=Path("data") / "coinlore_candles.csv",
		help="Path to OHLC candles CSV (set empty string to skip)",
	)
	parser.add_argument(
		"--candle-sec",
		type=int,
		default=60,
		help="Candle timeframe in seconds for local OHLC aggregation",
	)
	parser.add_argument("--yahoo-symbol", type=str, default="BTC-USD", help="Yahoo symbol, e.g. BTC-USD")
	parser.add_argument(
		"--yahoo-interval",
		type=str,
		default="1h",
		help="Yahoo interval (examples: 1m, 5m, 15m, 1h, 1d)",
	)
	parser.add_argument(
		"--yahoo-range",
		type=str,
		default="1mo",
		help="Yahoo range when start/end are not provided (examples: 7d, 1mo, 6mo, 1y, max)",
	)
	parser.add_argument(
		"--start-date",
		type=str,
		default=None,
		help="Start date in YYYY-MM-DD (for Yahoo custom period)",
	)
	parser.add_argument(
		"--end-date",
		type=str,
		default=None,
		help="End date in YYYY-MM-DD (for Yahoo custom period)",
	)
	parser.add_argument(
		"--windows-output",
		type=Path,
		default=Path("data") / "coinlore_windows.csv",
		help="Path to supervised windows CSV (set empty string to skip)",
	)
	parser.add_argument("--lookback", type=int, default=30, help="Number of lag prices in one sample")
	parser.add_argument("--horizon", type=int, default=1, help="Forecast horizon in steps")
	parser.add_argument("--timeout", type=int, default=15, help="HTTP timeout seconds")
	parser.add_argument("--retries", type=int, default=3, help="Retry count for API requests")

	args = parser.parse_args()
	candles_output = None if str(args.candles_output).strip() == "" else args.candles_output
	windows_output = None if str(args.windows_output).strip() == "" else args.windows_output

	if args.interval_sec <= 0:
		raise ValueError("--interval-sec must be > 0")
	if args.samples <= 0:
		raise ValueError("--samples must be > 0")
	if args.lookback <= 0:
		raise ValueError("--lookback must be > 0")
	if args.horizon <= 0:
		raise ValueError("--horizon must be > 0")
	if args.timeout <= 0:
		raise ValueError("--timeout must be > 0")
	if args.retries <= 0:
		raise ValueError("--retries must be > 0")
	if args.candle_sec < 0:
		raise ValueError("--candle-sec must be >= 0")
	if bool(args.start_date) != bool(args.end_date):
		raise ValueError("--start-date and --end-date must be provided together")
	if args.start_date and args.end_date:
		# Validate date format early.
		_ = parse_iso_date_to_epoch(args.start_date, is_end=False)
		_ = parse_iso_date_to_epoch(args.end_date, is_end=True)

	return CollectorConfig(
		source=args.source,
		coin_id=args.coin_id,
		interval_sec=args.interval_sec,
		samples=args.samples,
		output=args.output,
		candles_output=candles_output,
		candle_sec=args.candle_sec,
		yahoo_symbol=args.yahoo_symbol.strip(),
		yahoo_interval=args.yahoo_interval.strip(),
		yahoo_range=args.yahoo_range.strip(),
		start_date=args.start_date,
		end_date=args.end_date,
		windows_output=windows_output,
		lookback=args.lookback,
		horizon=args.horizon,
		timeout=args.timeout,
		retries=args.retries,
	)


def main() -> int:
	try:
		config = parse_args()

		if config.source == "coinlore":
			collected_rows = collect_timeseries(config)

			raw_fieldnames = [
				"fetched_at_utc",
				"coin_id",
				"symbol",
				"name",
				"rank",
				"price_usd",
				"price_btc",
				"volume24",
				"market_cap_usd",
				"percent_change_1h",
				"percent_change_24h",
				"percent_change_7d",
				"ts_api",
				"source",
			]
			written = append_rows_to_csv(config.output, collected_rows, raw_fieldnames)
			print(f"[OK] wrote {written} rows to: {config.output}")

			if config.candles_output is not None and config.candle_sec > 0:
				all_rows = read_rows(config.output)
				candles = build_candles(all_rows, candle_sec=config.candle_sec)
				n_candles = save_candles_csv(config.candles_output, candles)
				print(
					f"[OK] wrote {n_candles} candles to: {config.candles_output} "
					f"(interval={config.candle_sec}s)"
				)
			else:
				print("[INFO] candles generation skipped")

			if config.windows_output is not None:
				all_rows = read_rows(config.output)
				windows = build_windows(
					all_rows,
					lookback=config.lookback,
					horizon=config.horizon,
					price_key="price_usd",
					ts_key="fetched_at_utc",
				)
				n_windows = save_windows_csv(config.windows_output, windows, config.lookback)
				print(f"[OK] wrote {n_windows} supervised windows to: {config.windows_output}")
			else:
				print("[INFO] windows generation skipped")

		elif config.source == "yahoo":
			print(
				f"[INFO] Download Yahoo history for {config.yahoo_symbol}, "
				f"interval={config.yahoo_interval}"
			)
			yahoo_rows = fetch_yahoo_history(
				symbol=config.yahoo_symbol,
				interval=config.yahoo_interval,
				range_value=config.yahoo_range,
				start_date=config.start_date,
				end_date=config.end_date,
				timeout=config.timeout,
				retries=config.retries,
			)

			yahoo_fieldnames = [
				"timestamp_utc",
				"symbol",
				"interval",
				"open",
				"high",
				"low",
				"close",
				"volume",
				"source",
			]
			written = write_rows_to_csv(config.output, yahoo_rows, yahoo_fieldnames)
			print(f"[OK] wrote {written} Yahoo candles to: {config.output}")

			if config.windows_output is not None:
				all_rows = read_rows(config.output)
				windows = build_windows(
					all_rows,
					lookback=config.lookback,
					horizon=config.horizon,
					price_key="close",
					ts_key="timestamp_utc",
				)
				n_windows = save_windows_csv(config.windows_output, windows, config.lookback)
				print(f"[OK] wrote {n_windows} supervised windows to: {config.windows_output}")
			else:
				print("[INFO] windows generation skipped")

			if config.candles_output is not None and config.candles_output != config.output:
				copy_count = write_rows_to_csv(config.candles_output, yahoo_rows, yahoo_fieldnames)
				print(f"[OK] duplicated Yahoo candles to: {config.candles_output} ({copy_count} rows)")
		else:
			raise ValueError(f"Unsupported source: {config.source}")

		return 0
	except KeyboardInterrupt:
		print("\n[INFO] interrupted by user")
		return 130
	except Exception as err:
		print(f"[ERROR] {err}")
		return 1


if __name__ == "__main__":
	sys.exit(main())
