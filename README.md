# cc-email-capture

[![Build Status](https://travis-ci.org/hmrc/cc-email-capture.svg?branch=master)](https://travis-ci.org/hmrc/cc-email-capture) [ ![Download](https://api.bintray.com/packages/hmrc/releases/cc-email-capture/images/download.svg) ](https://bintray.com/hmrc/releases/cc-email-capture/_latestVersion)


The Childcare Calculator email capture process enables the user of the
Childcare Calculator to record their interest in receiving future
information updates, when Tax Free Childcare or additional free childcare
hours becomes available.

The user records their request for more information from the results
page of the Childcare calculator.
The page presented, allows the user to add their email address and
optionally to record their child's date of birth.

The email address, Child's date of birth and a free hours entitlement
indicator will be held on a Mongo database.

Prior to the email address being recorded, it is passed to the HMRC Email
Service to validate if the address is genuine.
If it is genuine, the data will be recorded and an email will be fired
off to the user from the HMRC Email service([HMRC Email Service documentation](https://github.tools.tax.service.gov.uk/HMRC/email/blob/master/README.md)),
stating that their request has been saved.

If there is an issue with the validation of the email address which has
been input, an error message will be presented on the page, asking for the
user to re-enter a genuine email address.


## Endpoint URL : /cc-email-capture


## Port Number : 9369


## Method

  All requests are of type `POST`.


## License

  This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
