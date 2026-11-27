FROM restic/restic:0.20.0 AS restic

FROM postgres:17.12-alpine

RUN apk upgrade --no-cache libxml2

COPY --from=restic /usr/bin/restic /usr/local/bin/restic
COPY backup.sh restore-verify.sh /usr/local/bin/

RUN chmod 0555 /usr/local/bin/backup.sh /usr/local/bin/restore-verify.sh

ENTRYPOINT []
CMD ["/usr/local/bin/backup.sh"]
