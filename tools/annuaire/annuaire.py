from flask import Flask, jsonify
import csv

app = Flask(__name__)
CSV_DIR = "/config" #cf configmap

@app.get("/annuaire")
def getJson():
    filename = "rabbitmq.clients-configuration.csv"
    path = os.path.join(CSV_DIR, filename)
    if not os.path.exists(path):
        abort(500, description="Fichier CSV introuvable")

    data = []
    with open(filename, newline='') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            data.append(row)

    data = cleanData(data)
    return jsonify(data)

def cleanData(data):
    """ On enlève les lignes et les colonnes du csv qui ne nous intéressent pas"""
    dataUpdated = [row for row in data if (row['CommonName'] != '' and row['CommonName'] != None)] # on supprime les éléments n'ayant pas de CommonName
    for row in dataUpdated:
        row['useXML'] = False if row['useXML'] == '' else True
        row['selected'] = False # pour les checkboxes de l'annuaire
        row['P1515'] = getVhost('15-15', row['P: 15-15'].split(';') if row['P: 15-15'] else [])
        row['P15gps'] = getVhost('15-gps', row['P: 15-gps'].split(';') if row['P: 15-gps'] else [])
        row['P15nexsis'] = getVhost('15-nexsis', row['P: 15-nexsis'].split(';') if row['P: 15-nexsis'] else [])
        row['P15smur'] = getVhost('15-smur', row['P: 15-smur'].split(';') if row['P: 15-smur'] else [])
    columns_to_remove = ['CommonName', 'additionalPermissions', 'lrm_test', 'directCISU', 'P: 15-15', 'P: 15-gps', 'P: 15-nexsis', 'P: 15-smur'] # on supprime les colonnes non utilisées par l'annuaire
    for row in dataUpdated:
        for column in columns_to_remove:
            if column in row:
                del row[column]
    return dataUpdated
    


def getVhost(vhost_prefix, versions):
    """ vhost_prefix is one of : '15-15', '15-gps', '15-nexsis', '15-smur'
        versions : string[]
    --------------------------------------------------------------------------------
        return : [[vhost, mdd], [vhost, mdd], ...]"""
    liste_vhost_mdd=[]
    for version in versions:
        liste_vhost_mdd.append([f"{vhost_prefix}_v{version}", getMDD(f"{vhost_prefix}_v{version}")])
    return liste_vhost_mdd

def getMDD(vhost):
    for raw in dsf:
        if raw['vhost'] == vhost:   # ATTENTION : problème pour le vhost: 15-nexsis_v0.9
            return raw['mdd']
            break
    return "N/A"

dsf = [
        { "perimeter": "P1515", "version": "1.5", "mdd": "1.0", "vhost": "15-15_v1.5"},
        { "perimeter": "P1515", "version": "2.0", "mdd": "2.0", "vhost": "15-15_v2.0" },
        { "perimeter": "P1515", "version": "2.1", "mdd": "3.0", "vhost": "15-15_v2.1" },
        { "perimeter": "P15smur", "version": "1.4", "mdd": "1.0", "vhost": "15-smur_v1.4" },
        { "perimeter": "P15smur", "version": "1.5", "mdd": "1.0", "vhost": "15-smur_v1.5" },
        { "perimeter": "P15smur", "version": "1.6", "mdd": "2.0", "vhost": "15-smur_v1.6" },
        { "perimeter": "P15smur", "version": "1.7", "mdd": "3.0", "vhost": "15-smur_v1.7" },
        { "perimeter": "P15gps", "version": "1.0", "mdd": "1.0", "vhost": "15-gps_v1.0" },
        { "perimeter": "P15gps", "version": "1.1", "mdd": "1.0", "vhost": "15-gps_v1.1" },
        { "perimeter": "P15gps", "version": "1.2", "mdd": "2.0", "vhost": "15-gps_v1.2" },
        { "perimeter": "P15gps", "version": "1.3", "mdd": "3.0", "vhost": "15-gps_v1.3" },
        { "perimeter": "P15nexsis", "version": "1.8", "mdd": "1.0", "vhost": "15-nexsis_v1.8" },
        { "perimeter": "P15nexsis", "version": "1.9", "mdd": "3.0", "vhost": "15-nexsis_v1.9" },
        { "perimeter": "P15nexsis", "version": "1.9.1", "mdd": "3.0", "vhost": "15-nexsis_v1.9.1"}
    ]

@app.get("/dsf")
def getDsf():
    return dsf

@app.get("/url")
def getUrl():
    return {
            "bas" : "https://messaging.bac-a-sable.hub.esante.gouv.fr/rabbitmq",
            "pprod": "https://messaging.pre-prod.hub.esante.gouv.fr/rabbitmq",
            "prod": "https://messaging.hub.esante.gouv.fr/rabbitmq",        
        }