# Offer Service

An example of a Scala project implementing a simple REST service using http4s, a functional http library that works with Cats & Cats-effects

## Notes
- I used property testing & ScalaCheck to begin with, but it's massive overkill here, since there is no interesting logic at play. Hence just standard unit tests.
- http4s seems nice and uncluttered, but apparently isn't as fast as Finangle/Finch (but is marginally quicker than AkkaHttp/Spray). Main downside is relative instability compared to Akka and resultant documentation/api problems
- pagination, authorisation and authentication would all be required for production
- it is assumed that offers should not be permitted to have negative price, but 0 is allowed
- cancellation is implemented as delete, if cancellation-undo were required then cancellation would have to be implemented as a sort of update - and perhaps with some eventual delete to manage the size of the storage
- if cancellation were no longer implemented as delete, the Http method should probably change
- time is awkward, a full implementation should probably use a remote clock to stop the system messing things up
- seconds have been chosen for granularity of time - might want more/less precision
- should verify non-negative expirey times really - but they will just get cleared up on the next flush

### Usage

Run: `sbt run`
Tests: `sbt test`

Create an offer:

```curl -X POST \
  http://localhost:8080/offers \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "offer name",
    "description": "desc",
    "price": 10,
    "secondsToExpiry": 40
}'```

(an id is returned)

List offers:

`curl http://localhost:8080/offers`

Get an offer by id:

`curl http://localhost:8080/offers/{id}`

Delete an offer by id:

`curl -X DELETE http://localhost:8080/offers/{id}`