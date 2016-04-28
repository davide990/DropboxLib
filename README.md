# Welcome
DropboxLib is a simple wrapper for dropbox library to upload and download files using java

## Examples of usage

### snippet 1
```
Dropbox dp = new Dropbox("YOUR_APP_KEY","YOUR_APP_SECRET_KEY");
dp.DoWebAuthentication();
dp.uploadFile("your_file");
```

### snippet 2
```
Dropbox dp = new Dropbox("YOUR_APP_KEY","YOUR_APP_SECRET_KEY");
dp.AuthenticateUsingAccessToken("your_access_token_here");
dp.uploadFile("your_file");
```

### snippet 3
```
Dropbox dp = new Dropbox("YOUR_APP_KEY","YOUR_APP_SECRET_KEY");
dp.AuthenticateUsingAuthCode("your_authorization_code_here");
dp.uploadFile("your_file");
```

### snippet 4
```
dp.downloadFile("/remote/file/path", "/local/destination/file/path");
```

### snippet 5
```
DbxEntry.File my_file = dp.uploadFile("your_file");
String public_link = dp.GetPublicLink(my_file.path);
```