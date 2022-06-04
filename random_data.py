import requests
import random
import dateutil.parser
import uuid
import json

def random_credit_card_number():
    def part():
        return f"{random.randint(0,9)}{random.randint(0,9)}{random.randint(0,9)}{random.randint(0,9)}"
    return f"{part()} {part()} {part()} {part()}"

clients = []

resp = requests.get(url="https://randomuser.me/api/?results=200")
results = resp.json()['results']

for data in results:
    id = data['login']['uuid']
    gender = data['gender']
    first_name = data['name']['first']
    last_name = data['name']['last']
    address = ', '.join([data['location']['street']['name'] + ' ' + str(data['location']['street']['number']), data['location']['city'], data['location']['state'] + ' ' + str(data['location']['postcode'])])
    phone_number = data['cell']
    email = data['email']
    birth_date = int(dateutil.parser.isoparse(data['dob']['date']).timestamp())

    if gender == 'male':
        gender = 'M'
    elif gender == 'female':
        gender = 'F'
    if random.randint(0,100) < 5:
        gender = '-'

    credit_cards = []
    for cc in (random_credit_card_number() for _ in range(random.randint(0,4))):
        credit_cards.append(cc)

    client = {
        "id": id,
        "gender": gender,
        "firstName": first_name,
        "lastName": last_name,
        "address": address,
        "phoneNumber": phone_number,
        "email": email,
        "birthDate": birth_date,
        "deleted": False,
        "creditCards": credit_cards
    }
    clients.append(client)

print(json.dumps(clients))
