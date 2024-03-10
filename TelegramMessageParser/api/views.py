from django.http import JsonResponse
from django.shortcuts import render
import configparser
import json
import asyncio
from datetime import datetime
from django.views.decorators.csrf import csrf_exempt
from telethon import TelegramClient
from telethon.errors import SessionPasswordNeededError
from telethon.tl.functions.messages import GetHistoryRequest
from telethon.tl.types import PeerChannel
import schedule
import time
import pytz

class DateTimeEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, datetime):
            # Localize datetime to UTC+5 (Asia/Tashkent time zone in this example)
            localized_datetime = o.replace(tzinfo=pytz.utc).astimezone(pytz.timezone('Asia/Tashkent'))
            return localized_datetime.isoformat()
        if isinstance(o, bytes):
            return list(o)
        return json.JSONEncoder.default(self, o)

def load_config():
    config = configparser.ConfigParser()
    config.read("config.ini")
    return config

def get_telegram_config(config):
    try:
        api_id = config['Telegram']['api_id']
        api_hash = config['Telegram']['api_hash']
        phone = config['Telegram']['phone']
        username = config['Telegram']['username']
        url = config['Telegram']['url']
        return api_id, api_hash, phone, username, url
    except KeyError as e:
        print(f"Error: {e}. Please make sure 'Telegram' section and keys 'api_id', 'api_hash', 'phone', 'username' are present in config.ini.")
        return None, None, None, None, None

@csrf_exempt
def fetchMessages(request):
    if request.method == 'GET':
        config = load_config()
        api_id, api_hash, phone, username, url = get_telegram_config(config)
        
        if not api_id or not api_hash or not phone or not username or not url:
            return JsonResponse({"error": "Telegram configuration missing or invalid"}, status=500)

        async def fetch_and_save_messages(api_id, api_hash, phone, username, url):
            client = TelegramClient(username, api_id, api_hash)

            try:
                await client.start()

                if not await client.is_user_authorized():
                    await client.send_code_request(phone)
                    try:
                        await client.sign_in(phone, input('Enter the code: '))
                    except SessionPasswordNeededError:
                        await client.sign_in(password=input('Password: '))

                user_input_channel = url

                if user_input_channel.isdigit():
                    entity = PeerChannel(int(user_input_channel))
                else:
                    entity = user_input_channel

                my_channel = await client.get_entity(entity)

                # Set limit to a high number to fetch all available messages
                limit = 10000
                all_messages = []
                

                history = await client(GetHistoryRequest(
                    peer=my_channel,
                    offset_id=0,
                    offset_date=None,
                    add_offset=0,
                    limit=limit,
                    max_id=0,
                    min_id=0,
                    hash=0
                ))

                messages = history.messages
                messages.reverse()
                for message in messages:
                    localized_date = message.date.replace(tzinfo=pytz.utc).astimezone(pytz.timezone('Asia/Tashkent'))
                    formatted_date = localized_date.isoformat()[:-6]
                    all_messages.append({
                        "id": message.id,
                        "date": formatted_date,
                        "message": message.message
                    })
                
                return all_messages

            except Exception as e:
                print(f"Error while fetching and saving messages: {e}")

            finally:
                await client.disconnect()

        fetched_messages = asyncio.run(fetch_and_save_messages(api_id, api_hash, phone, username, url))
        if fetched_messages:
            return JsonResponse(fetched_messages, status=200, safe=False)
        else:
            return JsonResponse({"error": "Failed to fetch messages"}, status=500)


    return JsonResponse({"error": "Invalid request method"}, status=405)
