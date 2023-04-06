#!/usr/bin/env python3

import requests
print('starting script...\n\n\n')
url_fetch_pending = 'http://pleo-antaeus:7000/rest/v1/invoices/pending'
url_charge = 'http://pleo-antaeus:7000/rest/v1/billing/'


invoices = requests.get(url_fetch_pending)
json_data = invoices.json()
if not json_data:
    print('All invoices were paid.')
    exit
for inv in json_data:
    print(f'Trying to charge: {url_charge}{inv.get("id")}')
    response = requests.post(f'{url_charge}{inv.get("id")}')
    response_text = response.text
    print(f'{response}: {response_text}\n')
