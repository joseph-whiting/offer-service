# Offer Service

An example of a Scala project implementing a simple REST service using http4s, a functional http library that works with Cats & Cats-effects

## Notes
- I used property testing & ScalaCheck to begin with, but it's massive overkill here, since there is no interesting logic at play. Hence just standard unit tests.
- http4s seems nice and uncluttered, but apparently isn't as fast as Finangle/Finch (but is marginally quicker than AkkaHttp/Spray)
- pagination, authorisation and authentication would all be required for production
- it is assumed that offers should not be permitted to have negative price, but 0 is allowed
- cancellation is implemented as delete, if cancellation-undo were required then cancellation would have to be implemented as a sort of update - and perhaps with some eventual delete to manage the size of the storage
- if cancellation were no longer implemented as delete, the Http method should probably change
- time is awkward, a full implementation should probably use a remote clock to stop the system messing things up
