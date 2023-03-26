# Helicopters investigation
## Objective
The goal is to share location and status of helicopters (SAMU, sécurité civile, ...) to LRM in near real-time.

## Approach
To do so in an iterative fashion, the goal is to use a lower quality high-availability source (RadarBox) and, when possible, use higher quality info coming from the LRM directly. 
We explore here the scrapping of RadarBox to get a base layer of information. 
Using their connection to the Hub Santé, LRM will also be able to push helicopters info to the Hub Santé.

## Solution
With this info collected, we will (every minute):
- get the latest information available for each helicopter from all sources combined -> base data
- get the LRM info on each helicopter in a 5' time window and overwrite the base data -> enriched data
- send the enriched data to all LRM connected to the Hub

## Prerequisite
- Discuss with HeliSMUR
- LRMs need to connect to the Hub Santé
- Industrialise and deploy (RadarBox scrapping, LRM info collection, enriched data computation and sending)
