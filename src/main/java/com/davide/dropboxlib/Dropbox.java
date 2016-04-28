package com.davide.dropboxlib;

import com.dropbox.core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple interface to Dropbox storage service.
 *
 * @author Davide Guastella <davide.guastella90@gmail.com>
 */
public class Dropbox
{

    /**
     * Interface to dropbox
     */
    DbxClient dbx_client = null;

    /**
     * User related access token
     */
    private String access_token = null;

    /**
     * App key
     */
    private String APP_KEY;

    /**
     * Secret key
     */
    private String APP_SECRET;

    /**
     * An identifier for the API client, typically of the form "Name/Version".
     */
    private final String client_identifier;

    /**
     * A container for configuration parameters for how requests to the Dropbox
     * servers should be made
     */
    private final DbxRequestConfig config;
    private boolean enable_log;
    private Logger logger;

    public Dropbox()
    {
        SetLogEnabled(true);

        this.client_identifier = "dbx_utility_lib/0.1";
        config = new DbxRequestConfig(client_identifier, Locale.getDefault().toString());
    }

    /**
     * Create a new instance of Dropbox utility
     *
     * @param app_key
     * @param app_secret_key
     */
    public Dropbox(String app_key, String app_secret_key)
    {
        SetLogEnabled(true);

        this.client_identifier = "dbx_utility_lib/0.1";
        config = new DbxRequestConfig(client_identifier, Locale.getDefault().toString());
        this.set_APP_KEY(app_key);
        this.set_APP_SECRET(app_secret_key);

    }

    /**
     * Do web authentication for dropbox services. Use this method when the
     * access token is unavaible.
     *
     * @throws IOException
     * @throws DbxException
     */
    public void DoWebAuthentication() throws IOException, DbxException
    {
        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
        String code = null;

        // Have the user sign in and authorize your app.
        String authorizeUrl = webAuth.start();

        //Open the default system browser to the authorization url
        if (Desktop.isDesktopSupported())
        {
            try
            {
                Desktop.getDesktop().browse(new URI(authorizeUrl));
            } catch (IOException | URISyntaxException e)
            {
                System.out.println("Error while opening default system browser. DBX Authorization URL: " + authorizeUrl);
                e.printStackTrace();
            }
        }

        //If GUI is avaible, show an input dialog for inserting the authorization code
        if (GraphicsEnvironment.isHeadless())
        {
            System.out.println("Insert here the received authorization code");
            code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
        } else
        {
            code = AuthCodeInput();
        }

        // This will fail if the user enters an invalid authorization code.
        DbxAuthFinish authFinish = webAuth.finish(code);

        // Set the access token
        this.access_token = authFinish.accessToken;

        // Create the interface to dropbox
        dbx_client = new DbxClient(config, this.access_token);

        log("Linked account: " + dbx_client.getAccountInfo().displayName);
    }

    /**
     * Authenticate into dropbox by using an authorization code.
     *
     * @param code
     * @throws IOException
     * @throws DbxException
     */
    public void AuthenticateUsingAuthCode(String code) throws IOException, DbxException
    {
        // Set the access token
        this.access_token = GetAccessToken(code);

        // Create the interface to dropbox
        dbx_client = new DbxClient(config, this.access_token);

        log("Linked account: " + dbx_client.getAccountInfo().displayName);
    }

    /**
     * Do authentication using a specified access token.
     *
     * @param accessToken the access token key.
     *
     * @return
     */
    public boolean AuthenticateUsingAccessToken(String accessToken)
    {
        // Set the access token
        this.access_token = accessToken;

        boolean success = true;

        try
        {
            // Create the interface to dropbox
            dbx_client = new DbxClient(config, this.access_token);
            log("Linked account: " + dbx_client.getAccountInfo().displayName);
        } catch (DbxException e)
        {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    /**
     * Retrieve the access token using a specified authorization code.
     *
     * @param auth_code
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    private String GetAccessToken(String auth_code) throws ClientProtocolException, IOException
    {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("https://api.dropbox.com/1/oauth2/token");
        List<NameValuePair> nameValuePairs = new ArrayList<>(1);
        String grant_type = "authorization_code";
        String theAccessToken = null;

        nameValuePairs.add(new BasicNameValuePair("code", auth_code));
        nameValuePairs.add(new BasicNameValuePair("grant_type", grant_type));
        nameValuePairs.add(new BasicNameValuePair("client_id", this.APP_KEY));
        nameValuePairs.add(new BasicNameValuePair("client_secret", this.APP_SECRET));

        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse response = client.execute(post);
        String entity_string = EntityUtils.toString(response.getEntity());

        try
        {
            JSONObject json = new JSONObject(entity_string);
            theAccessToken = json.getString("access_token");
        } catch (JSONException e)
        {
            e.printStackTrace();
        }

        return theAccessToken;
    }

    /**
     * Show an input dialog to let the user to insert the code for authorizing
     * dropbox application.
     *
     * @return the authorization code the user wrote.
     */
    private String AuthCodeInput()
    {
        JFrame frame = new JFrame("Dropbox");

        // prompt the user to enter the auth code
        return JOptionPane.showInputDialog(frame, "Insert here the received authorization code");
    }

    /**
     * Upload a file
     *
     * @param file_path
     * @return the uploaded file object
     *
     * @throws DbxException
     * @throws IOException
     */
    public DbxEntry.File Upload(String file_path) throws DbxException, IOException
    {
        File inputFile = new File(file_path);
        FileInputStream inputStream = new FileInputStream(inputFile);
        DbxEntry.File uploadedFile = null;
        try
        {
            //Get the file name without the absolute path
            String dest_fname = "/" + inputFile.getName();

            log("Saving " + file_path + " into [DBX]" + dest_fname);

            //Store the file into the remote root directory
            uploadedFile = dbx_client.uploadFile(dest_fname, DbxWriteMode.add(), inputFile.length(), inputStream);

            log("Uploaded: " + uploadedFile.toString());
        } finally
        {
            inputStream.close();
        }

        return uploadedFile;
    }

    /**
     * Upload a file
     *
     * @param file_path	the file name to upload
     * @param dest_file_path the remote file name
     * @return the uploaded file object
     * @throws DbxException
     * @throws IOException
     */
    public DbxEntry.File Upload(String file_path, String dest_file_path) throws DbxException, IOException
    {
        File inputFile = new File(file_path);
        FileInputStream inputStream = new FileInputStream(inputFile);
        DbxEntry.File uploadedFile = null;
        try
        {
            log("Saving " + file_path + " into [DBX]" + dest_file_path);

            //Store the file into the remote root directory
            uploadedFile = dbx_client.uploadFile(dest_file_path, DbxWriteMode.add(), inputFile.length(), inputStream);

            log("Uploaded: " + uploadedFile.toString());
        } finally
        {
            inputStream.close();
        }

        return uploadedFile;
    }

    /**
     * Upload a file
     *
     * @param file_path	the file name to upload
     * @param dest_file_path the remote file name
     * @param writeMode specify the write mode for the file to be uploaded (See
     * 'DbxWriteMode' class reference)
     *
     * @return the uploaded file object
     * @throws DbxException
     * @throws IOException
     */
    public DbxEntry.File Upload(String file_path, String dest_file_path, DbxWriteMode writeMode) throws DbxException, IOException
    {
        File inputFile = new File(file_path);
        FileInputStream inputStream = new FileInputStream(inputFile);
        DbxEntry.File uploadedFile = null;

        try
        {
            log("Saving " + file_path + " into [DBX]" + dest_file_path);

            //Store the file into the remote root directory
            uploadedFile = dbx_client.uploadFile(dest_file_path, writeMode, inputFile.length(), inputStream);

            log("Uploaded: " + uploadedFile.toString());
        } finally
        {
            inputStream.close();
        }

        return uploadedFile;
    }

    /**
     * Download a file
     *
     * @param remote_file_path
     * @param dest_file_path
     * @throws DbxException
     * @throws IOException
     */
    public void Download(String remote_file_path, String dest_file_path) throws DbxException, IOException
    {
        DbxEntry.WithChildren listing = dbx_client.getMetadataWithChildren("/");

        log("Files in the root path:");

        for (DbxEntry child : listing.children)
        {
            log("	" + child.name + ": " + child.toString());
        }

        FileOutputStream outputStream = new FileOutputStream(dest_file_path);
        try
        {
            DbxEntry.File downloadedFile = dbx_client.getFile(remote_file_path, null, outputStream);
            log("Metadata: " + downloadedFile.toString());
        } finally
        {
            outputStream.close();
        }
    }

    /**
     * Get a public access link for a remote file.
     *
     * @param fname
     * @return
     */
    public String GetPublicLink(String fname)
    {
        try
        {
            return dbx_client.createShareableUrl(fname);
        } catch (DbxException e)
        {
            log("Unable to get the shareable URL for file " + fname);
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Log a message
     *
     * @param msg
     */
    private void log(String msg)
    {
        if (IsLogEnabled())
        {
            logger.info("[DROPBOX] " + msg);
        }
    }

    public String get_access_token()
    {
        return access_token;
    }

    public String get_APP_KEY()
    {
        return APP_KEY;
    }

    private void set_APP_KEY(String aPP_KEY)
    {
        APP_KEY = aPP_KEY;
    }

    public void set_APP_SECRET(String aPP_SECRET)
    {
        APP_SECRET = aPP_SECRET;
    }

    public boolean IsLogEnabled()
    {
        return enable_log;
    }

    public void SetLogEnabled(boolean enable_log)
    {
        this.enable_log = enable_log;
    }

}
