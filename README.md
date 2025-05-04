#### IMPORTANT INFORMATION ####
This application currently will not run due to the use of a research-based library
being used as an internal private package. If you are interested in running the example
application or seeing a demonstration, please reach out to (the library creator)[zg009@uark.edu].

### Instructions ###
To use this application, you first need to install the models from the
Google AI Edge RAG tutorial https://ai.google.dev/edge/mediapipe/solutions/genai/rag/android

You must load these into the corresponding folders in your device or emulator using adb

You also need to install the (ntfy.sh)[https://play.google.com/store/apps/details?id=io.heckel.ntfy] 
app in order to use the (UnifiedPush protocol)[https://unifiedpush.org/] which allows communication from the user's Solid pod
to the application on the phone, and the broadcast receiver which runs in the RAG service app and correctly
routes queries to the calling app.

To have the routes successfully delegated, you must run the solid_query_engine.py file as well. 
The order to start these at this time is load the service and attach to the io.henckel.ntfy service,
and then navigate to the ntfy.sh app. Copy that URL into the environment for the python file, and begin 
executing the Python program with the proper credentials. You should then be able to query the RAG service
once service initialization on the device has finalized, and should see responses being routed from the Python 
service back to your device. In the future, we hope to streamline and automate this process to make it less
cumbersome for users to execute.

## Information ##
This application is used for the workshop GOBLIN 2025 conference. It is an Android application
which makes use of the Solid protocol to delegate requests to a RAG model running locally against the
user's Pod resources and serving responses back to the model on the user's Android device. If connection
is unavailable, the query is delegated to the user's on-device RAG model.

## Branches ##
```main``` - This includes the code for the RAG service and application which users can use.
```evaluate-rag-service``` - This is used to evaluate the queries and record the responses from the Gemma models.