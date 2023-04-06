#!/usr/bin/env python3

import sys
import requests

persistent = False
print(sys.argv)
if len(sys.argv) > 1 and sys.argv[1] == '--persistent':
    persistent = True
elif len(sys.argv) > 1:
    print('Usage: python scheduler.py [--persistent]\n')
    print('Execute the payment of all invoices.\n')
    print('NOTE: This script will only run once. Since there may be errors processing payments, consider using the --persistent flag to pay all invoices.\n')
    print('Options:')
    print('  --persistent  Run the script continuously until every invoice is paid.\n')
    sys.exit()

url = 'http://pleo-antaeus:7000/rest/v1/billing'

paid_all_invoices_message = 'Successfully charged all pending invoices'
no_pending_invoices = 'There was a problem: No pending invoices found'

while True:
    response = requests.post(url)
    if response.status_code in [200, 500]:
        response_text = response.text
        print(response_text)
        if persistent and (paid_all_invoices_message in response_text or no_pending_invoices in response_text):
            break
        elif not persistent:
            break
