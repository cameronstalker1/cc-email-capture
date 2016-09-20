#!/bin/sh

SCRIPT=$(find . -type f -name cc-email-capture)
exec $SCRIPT \
  $HMRC_CONFIG
