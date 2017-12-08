# personal-details-validation-frontend

[![Build Status](https://travis-ci.org/hmrc/personal-details-validation-frontend.svg)](https://travis-ci.org/hmrc/personal-details-validation-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/personal-details-validation-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/personal-details-validation-frontend/_latestVersion)

personal-details-validation-frontend service is used to capture name, surname, date of birth and nino of the user and make it available to all the services.

# API

| Path                                    | Description                              |
|:--------------------------------------- |:---------------------------------------- |
| GET /start?completionUrl=:completionUrl | Start capturing user's personal details  |
    
## GET /start?completionUrl=:completionUrl
Displays a page to capture user's details. After capturing user's details, these details are checked against citizen details database. 
If they match, this information is stored by [personal-details-validation](https://github.com/hmrc/personal-details-validation) backend service. Then the user is redirected to the completionUrl regardless of citizen details check outcome. 
validationId parameter will be appended to the completionUrl. It is a UUID and it can be used to retrieve the data later using [personal-details-validation](https://github.com/hmrc/personal-details-validation) backend service.
NOTE: User should be redirected to this page. It shouldn't be called directly from a micro-service.

### Parameters
| Name          | Description                                   |
|:------------- |:--------------------------------------------- |
| completionUrl | Mandatory. Should be a valid relative URL.    |
    
### Example redirects
| CompletionUrl                 | Redirect url                                                                  |
|:----------------------------- |:----------------------------------------------------------------------------- |
|/my-service/pdv-complete       | /my-service/pdv-complete?journeyId=0018941f-fed3-47db-a05c-8b55e941324b       |
|/my-service/pdv-complete?a=b   | /my-service/pdv-complete?a=b&journeyId=0018941f-fed3-47db-a05c-8b55e941324b   |
    
### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")