This is a simple application for analyze photos using the services in AWS.
It is made by looking at this tutorial:
https://github.com/awsdocs/aws-doc-sdk-examples/tree/master/javav2/usecases/creating_photo_analyzer_app

My version of this application differs in some small and some lager ways from the one in the tutorial. 
Some of the differences are:
 * The region and the from mail address are set in the properties file.
 * Logging of errors and some out-prints
 * Uses constructor-based DI instead of field based