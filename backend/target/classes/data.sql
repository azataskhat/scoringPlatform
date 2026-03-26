-- Seed OSINT Sources (idempotent via ON CONFLICT)
INSERT INTO osint_sources (name, type, base_url, api_key_ref, active, update_interval_minutes)
VALUES
    ('Shodan',    'search_engine',     'https://api.shodan.io',               'SHODAN_API_KEY',    true, 60),
    ('Censys',    'search_engine',     'https://search.censys.io/api/v2',     'CENSYS_API_KEY',    true, 120),
    ('GreyNoise', 'threat_intel',      'https://api.greynoise.io/v3',         'GREYNOISE_API_KEY', true, 90),
    ('NVD',       'vulnerability_db',  'https://services.nvd.nist.gov/rest/json/cves/2.0', 'NVD_API_KEY', true, 360)
ON CONFLICT (name) DO NOTHING;

-- Seed IoT Devices (Almaty region demo data)
INSERT INTO iot_devices (source_id, ip_address, port, protocol, device_type, manufacturer, firmware_version, city, latitude, longitude, raw_data)
SELECT s.id, d.ip, d.port, d.protocol, d.device_type, d.manufacturer, d.firmware, 'Almaty', d.lat, d.lon, d.raw
FROM osint_sources s,
(VALUES
    ('Shodan', '185.125.88.10',  1883, 'MQTT',   'sensor',  'Espressif',  '2.1.0', 43.2380, 76.9450, '{"banner":"Mosquitto MQTT"}'),
    ('Shodan', '185.125.88.22',  8883, 'MQTTS',  'gateway', 'Teltonika',  '7.4.1', 43.2510, 76.9230, '{"banner":"TLS MQTT Broker"}'),
    ('Shodan', '185.125.88.35',  502,  'Modbus', 'plc',     'Siemens',    '4.2.3', 43.2220, 76.8510, '{"banner":"Modbus/TCP"}'),
    ('Censys', '185.125.88.41',  80,   'HTTP',   'camera',  'Hikvision',  '5.7.2', 43.2650, 76.9700, '{"banner":"Hikvision IP Camera"}'),
    ('Censys', '185.125.88.55',  443,  'HTTPS',  'router',  'MikroTik',   '6.49',  43.2370, 76.8820, '{"banner":"RouterOS"}'),
    ('Censys', '185.125.88.60',  5683, 'CoAP',   'sensor',  'Nordic Semi','3.0.1', 43.2720, 76.9100, '{"banner":"CoAP Server"}'),
    ('GreyNoise','185.125.88.71', 23,  'Telnet', 'router',  'TP-Link',    '1.2.3', 43.2900, 76.9500, '{"noise":true}'),
    ('GreyNoise','185.125.88.82', 22,  'SSH',    'server',  'Ubiquiti',   '2.0.6', 43.2100, 76.8700, '{"classification":"benign"}')
) AS d(src, ip, port, protocol, device_type, manufacturer, firmware, lat, lon, raw)
WHERE s.name = d.src
ON CONFLICT DO NOTHING;

-- Seed Vulnerabilities
INSERT INTO vulnerabilities (device_id, cve_id, severity, cvss_score, description, source_id)
SELECT dev.id, v.cve, v.sev, v.cvss, v.descr, dev.source_id
FROM iot_devices dev,
(VALUES
    ('185.125.88.35', 'CVE-2022-45092', 'CRITICAL', 9.8, 'Siemens PLC remote code execution via Modbus'),
    ('185.125.88.41', 'CVE-2023-28808', 'HIGH',     8.2, 'Hikvision camera authentication bypass'),
    ('185.125.88.41', 'CVE-2021-36260', 'CRITICAL', 9.8, 'Hikvision command injection vulnerability'),
    ('185.125.88.55', 'CVE-2023-32154', 'HIGH',     7.5, 'MikroTik RouterOS DNS cache poisoning'),
    ('185.125.88.71', 'CVE-2022-30075', 'HIGH',     8.8, 'TP-Link router remote code execution'),
    ('185.125.88.10', 'CVE-2023-3028',  'MEDIUM',   5.3, 'Mosquitto MQTT broker DoS vulnerability')
) AS v(ip, cve, sev, cvss, descr)
WHERE dev.ip_address = v.ip
ON CONFLICT DO NOTHING;

-- Seed Scoring Results
INSERT INTO scoring_results (source_id, reliability_score, timeliness_score, completeness_score, accessibility_score, total_score, parameters, calculated_at)
SELECT s.id, sc.r, sc.t, sc.c, sc.a,
       (0.35*sc.r + 0.25*sc.t + 0.25*sc.c + 0.15*sc.a),
       sc.params, sc.ts
FROM osint_sources s,
(VALUES
    ('Shodan',    0.85, 0.78, 0.82, 0.91, '{"r1":0.88,"r2":0.80,"r3":0.87,"t1":0.75,"t2":0.81,"c1":0.85,"c2":0.78,"c3":0.83,"a1":0.95,"a2":0.87}', NOW() - INTERVAL '3 days'),
    ('Censys',    0.82, 0.72, 0.88, 0.85, '{"r1":0.84,"r2":0.78,"r3":0.84,"t1":0.70,"t2":0.74,"c1":0.90,"c2":0.85,"c3":0.89,"a1":0.88,"a2":0.82}', NOW() - INTERVAL '2 days'),
    ('GreyNoise', 0.75, 0.90, 0.65, 0.88, '{"r1":0.78,"r2":0.70,"r3":0.77,"t1":0.92,"t2":0.88,"c1":0.68,"c2":0.60,"c3":0.67,"a1":0.90,"a2":0.86}', NOW() - INTERVAL '1 day'),
    ('NVD',       0.95, 0.60, 0.92, 0.95, '{"r1":0.97,"r2":0.92,"r3":0.96,"t1":0.55,"t2":0.65,"c1":0.94,"c2":0.90,"c3":0.92,"a1":0.98,"a2":0.92}', NOW())
) AS sc(src, r, t, c, a, params, ts)
WHERE s.name = sc.src
ON CONFLICT DO NOTHING;

-- Seed Security Events
INSERT INTO security_events (device_id, event_type, severity, description, source_id)
SELECT dev.id, e.etype, e.sev, e.descr, dev.source_id
FROM iot_devices dev,
(VALUES
    ('185.125.88.35', 'new_vulnerability', 'CRITICAL', 'Critical RCE vulnerability detected on Siemens PLC'),
    ('185.125.88.41', 'new_exposure',      'HIGH',     'Hikvision camera exposed to public internet without auth'),
    ('185.125.88.71', 'port_change',       'MEDIUM',   'Telnet port opened on TP-Link router'),
    ('185.125.88.10', 'new_vulnerability', 'MEDIUM',   'MQTT broker vulnerable to DoS attack'),
    ('185.125.88.55', 'new_vulnerability', 'HIGH',     'RouterOS DNS cache poisoning possible')
) AS e(ip, etype, sev, descr)
WHERE dev.ip_address = e.ip
ON CONFLICT DO NOTHING;
