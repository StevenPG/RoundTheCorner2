#!/usr/bin/env python3
"""
Generate a GPX "drive" that approaches (or leaves) a point in a straight line,
for replaying in the Android Emulator: Extended controls (...) > Location >
Routes > Load GPX/KML > Play route.

Examples:
    # Drive towards 39.7392,-104.9903 from 5 km due south at 50 km/h:
    python3 tools/make_route_gpx.py 39.7392 -104.9903 --from-km 5 --bearing 180 > arrive.gpx

    # Leave it: start at the point and drive 3 km east at 30 km/h:
    python3 tools/make_route_gpx.py 39.7392 -104.9903 --from-km 3 --bearing 90 --leave --kmh 30 > leave.gpx

Only uses the standard library (spherical-earth math; ample for testing).
"""
import argparse
import math
from datetime import datetime, timedelta, timezone

EARTH_RADIUS_M = 6_371_008.8


def destination(lat, lng, bearing_deg, distance_m):
    """Point reached travelling distance_m from (lat, lng) on the given bearing."""
    d = distance_m / EARTH_RADIUS_M
    th = math.radians(bearing_deg)
    p1, l1 = math.radians(lat), math.radians(lng)
    p2 = math.asin(math.sin(p1) * math.cos(d) + math.cos(p1) * math.sin(d) * math.cos(th))
    l2 = l1 + math.atan2(math.sin(th) * math.sin(d) * math.cos(p1), math.cos(d) - math.sin(p1) * math.sin(p2))
    return math.degrees(p2), (math.degrees(l2) + 540) % 360 - 180


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("lat", type=float, help="target latitude (decimal degrees)")
    ap.add_argument("lng", type=float, help="target longitude (decimal degrees)")
    ap.add_argument("--from-km", type=float, default=5.0, help="how far away the route starts/ends (km)")
    ap.add_argument("--bearing", type=float, default=180.0, help="direction from the target to the far end (deg)")
    ap.add_argument("--kmh", type=float, default=50.0, help="speed (km/h)")
    ap.add_argument("--step-s", type=float, default=5.0, help="seconds between points")
    ap.add_argument("--leave", action="store_true", help="start at the target and drive away")
    args = ap.parse_args()

    total_m = args.from_km * 1000
    step_m = args.kmh / 3.6 * args.step_s
    steps = max(1, int(total_m / step_m))
    t0 = datetime.now(timezone.utc)

    print('<?xml version="1.0" encoding="UTF-8"?>')
    print('<gpx version="1.1" creator="RoundTheCorner2 make_route_gpx" xmlns="http://www.topografix.com/GPX/1/1">')
    print("  <trk><name>RoundTheCorner test drive</name><trkseg>")
    for i in range(steps + 1):
        frac = i / steps
        dist_from_target = total_m * (frac if args.leave else 1 - frac)
        lat, lng = destination(args.lat, args.lng, args.bearing, dist_from_target)
        when = (t0 + timedelta(seconds=i * args.step_s)).strftime("%Y-%m-%dT%H:%M:%SZ")
        print(f'    <trkpt lat="{lat:.6f}" lon="{lng:.6f}"><time>{when}</time></trkpt>')
    print("  </trkseg></trk>")
    print("</gpx>")


if __name__ == "__main__":
    main()
