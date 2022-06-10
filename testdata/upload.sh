#!/bin/bash

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

for f in $SCRIPTPATH/data/*.sql;
do
    PGPASSWORD=password psql \
      -h localhost \
      -p 5432 \
      -d kotlinvertx \
      -U username \
      -f "$f"
done
