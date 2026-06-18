Application Name: SmartScan Notes

Brief Description:
SmartScan Notes is an Android app that scans document text using Google ML Kit OCR,
summarizes it using an AI API via Retrofit, extracts simple keywords, and saves notes in SQLite.
The app includes Firebase email/password authentication, note search, and detail view.

Main Screens:
1) MainActivity (Login)
2) CreateAccountActivity (Signup)
3) NotesListActivity (Search + List + Logout)
4) CaptureActivity (Photo/Gallery + OCR + AI Summary + Save)
5) NoteDetailActivity (View saved note)

Setup Notes:
- Add your Firebase project and google-services.json in app/
- Enable Email/Password in Firebase Authentication
- add HUGGING_FACE_TOKEN 

