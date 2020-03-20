import csv
import json
import os
import pickle
import shelve
from collections import defaultdict
from typing import Dict, List, Union

import wget as wget


class Bidict(dict):
    """
    A dictionary that supports for bidirectional access for key and value pairs.
    Note: key and value should have no overlapping.
    """

    def __setitem__(self, key, val):
        dict.__setitem__(self, key, val)
        dict.__setitem__(self, val, key)

    def __delitem__(self, key):
        dict.__delitem__(self, self[key])
        dict.__delitem__(self, key)


def read_id_json(target='state') -> Dict[Union[str, int], Union[str, int]]:
    """
    Read from state/county json, or from a pickle cache if read before.
    return data structure is as followed:
        {
            "california": 6,
            6: "california"
        }
    :param target: 'state' or 'county'
    """
    os.makedirs("cache", exist_ok=True)
    try:
        with open(os.path.join("cache", f"{target if target == 'state' else 'contie'}s.pickle"), 'rb') as in_:
            return pickle.load(in_)
    except:
        with open(os.path.join('raw_id_jsons', f'{target}.json'), 'rb') as file:
            all_states = json.load(file)
        ids = Bidict()
        for feature in all_states['features']:
            id_, name = int(feature['properties'][f'{target}ID']), feature['properties']['name'].lower()
            ids[id_] = name
        with open(os.path.join("cache", f"{target if target == 'state' else 'contie'}s.pickle"), 'wb+') as out:
            pickle.dump(ids, out)
        return ids


def get_latest_csvs(file_names: List[str]):
    """
    Re-download the latest csv from JHU dataset, see https://github.com/CSSEGISandData/COVID-19 for more details
    :param file_names: list of csv file names
    """
    os.makedirs("temp", exist_ok=True)
    for file_name in file_names:
        try:
            os.remove(os.path.join('temp', file_name))
        except:
            pass
        print(f"---- DOWNLOADING {os.path.join('temp', file_name)}")
        wget.download(
            f"https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/"
            f"csse_covid_19_time_series/{file_name}", out=os.path.join('temp', file_name))


def _construct_default_dict():
    """
    Needed for shelve to work: it only can pickle function from global level.
    """
    return defaultdict(dict)


def get_cases_by_date(file_names: List[str], state_ids: Dict[Union[str, int], Union[str, int]]):
    """
    Extract cases by date. The returned data structure is as followed:
        {
            "3/19/20": {
                6: {
                    "confirmed": 1,
                    "recovered": 1,
                    "death": 0
                }
            }
        }
        where `6` is the id for California in the example.

    :param file_names: list of csv file names
    :param state_ids: Bidict for state id-name mapping
    """
    get_latest_csvs(file_names)

    cases_by_date = defaultdict(_construct_default_dict)
    for file_name in file_names:
        print(f"---- EXTRACTING {os.path.join('temp', file_name)}")
        with open(os.path.join('temp', file_name), 'r') as file:
            key_name = file_name.split('-')[-1].lower()[:-4]
            csv_reader = csv.DictReader(file)
            dates = list(filter(lambda x: x[-2:] == '20', csv_reader.fieldnames))

            for row in csv_reader:
                if row['Country/Region'] == 'US':
                    for date in dates:
                        cases_by_date[date][state_ids.get(row['Province/State'].lower())][key_name] = row[date]

    return cases_by_date


def get_latest_date(cases_by_date: Dict[str, Dict[int, Dict[str, int]]]):
    """
    find latest date in the dataset
    :param cases_by_date: returned structure as described in get_cases_by_date(2)
    """
    dates = list(cases_by_date.keys())
    return max(dates, key=lambda x: (int(x.split('/')[2]), int(x.split('/')[0]), int(x.split('/')[1])))


def write_to_csv(cases_by_date: Dict[str, Dict[int, Dict[str, int]]], target='state',
                 out=None) -> None:
    """
    Write csv file to disk. can specify the target and output path
    :param cases_by_date: returned structure as described in get_cases_by_date(2)
    :param target: 'state' or 'county'
    :param out: output csv file path, by default is f'{target}_cases.csv'
    """
    if out is None:
        out = f'{target}_cases.csv'
    print(f"---- WRITING {target}_cases TO DISK {out}")
    with open(out, 'w', newline='\n') as f:
        writer = csv.DictWriter(f, [f'{target}_id', 'last_update', 'confirmed', 'recovered', 'death'])
        writer.writeheader()
        for date, cases in cases_by_date.items():
            for state_id, counters in cases.items():
                row = {f'{target}_id': state_id,
                       'last_update': date,
                       'confirmed': counters['confirmed'],
                       'recovered': counters['recovered'],
                       'death': counters['deaths']}
                writer.writerow(row)
    print("DONE!")


def update_cache(cases_by_date: Dict[str, Dict[int, Dict[str, int]]], target='state'):
    """
    Update cache in case needed for later reference.
    :param cases_by_date: returned structure as described in get_cases_by_date(2)
    :param target: 'state' or 'county'
    """
    print(f"---- UPDATING CACHE FOR {target}")
    new_latest_date = get_latest_date(cases_by_date)
    with shelve.open(os.path.join('cache', f'last_updated_{target}_cache.shelve')) as cache:
        dates = list(cache.keys())
        latest_date = max(dates, key=lambda x: (int(x.split('/')[2]), int(x.split('/')[0]), int(x.split('/')[1])),
                          default=None)
        if new_latest_date != latest_date:
            cache[new_latest_date] = cases_by_date
        print(f"NOW CACHE HAS DATA FOR {list(cache.keys())}")


if __name__ == "__main__":
    file_names = ["time_series_19-covid-Confirmed.csv", "time_series_19-covid-Recovered.csv",
                  "time_series_19-covid-Deaths.csv"]

    state_ids = read_id_json('state')
    state_cases_by_date = get_cases_by_date(file_names, state_ids)

    update_cache(state_cases_by_date, 'state')

    write_to_csv(state_cases_by_date, 'state')
