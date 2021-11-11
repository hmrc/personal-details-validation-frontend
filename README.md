# personal-details-validation-frontend

personal-details-validation-frontend service is used to capture name, surname, date of birth and either nino or postcode of the user and make it available to all the services.


### Test
```
sbt test
```


# API

| Path                                    | Description                              |
|:--------------------------------------- |:---------------------------------------- |
| GET /personal-details-validation/start?completionUrl=:completionUrl&origin=test | Start capturing user's personal details  |
    
## GET /personal-details-validation/start?completionUrl=:completionUrl&origin=test
Displays a page to capture user's details. After capturing user's details, these details are checked against citizen details database. 
If they match, this information is stored by [personal-details-validation](https://github.com/hmrc/personal-details-validation) backend service and the user is redirected to the completionUrl with `validationId` query parameter. It is a UUID and it can be used to retrieve the validation outcome and personal-details (if validation was successful) later using [personal-details-validation](https://github.com/hmrc/personal-details-validation#get-personal-details-validationvalidationid) backend service.
If the details do not match, then the user is shown the blank form with an error at the top of the form.

If the user does not interact with the browser for 15 minutes, and they ignore the modal timeout dialog, then they will be redirected back to the completionUrl with the `userTimeout` query parameter.

Also, if there is technical error in personal-details-validation component, then user is redirected to the completionUrl with `technicalError` query parameter.

### Parameters
| Name          | Description                                   |
|:------------- |:--------------------------------------------- |
| completionUrl | Mandatory. Should be a url-encoded relative URL or starts with `http://localhost`.    |
| origin        | Option. Reporting origin as a custom dimension in PDV GA events.    |
    
### Example redirects
| CompletionUrl                 | Redirect url                                                                  |
|:----------------------------- |:----------------------------------------------------------------------------- |
|/my-service/pdv-complete       | /my-service/pdv-complete?validationId=0018941f-fed3-47db-a05c-8b55e941324b       |
|/my-service/pdv-complete?a=b   | /my-service/pdv-complete?a=b&validationId=0018941f-fed3-47db-a05c-8b55e941324b   |
|/my-service/pdv-complete?a=b   | /my-service/pdv-complete?a=b&technicalError                                      |
|/my-service/pdv-complete?a=b   | /my-service/pdv-complete?a=b&userTimeout                                         |

## Test Repositories

The personal details validation frontend service is tested by the [personal details validation acceptance tests](https://github.com/hmrc/personal-details-validation-acceptance-tests). If any changes are made to this service please run those tests before raising a PR. Information on how to run the tests are located in the respective repository readme.
