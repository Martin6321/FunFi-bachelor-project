package eu.mcomputing.cohave.funfi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.List;

import eu.mcomputing.cohave.authentication.AccountGeneral;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.introDownload.MyDownloadService;
import eu.mcomputing.cohave.funfi.logger.Logger;


/*
 The MIT License (MIT)

 Copyright (c) 2015 Maros Cavojsky (www.mpage.sk), mComputing (www.mcomputig.eu), mComputing (www.mcomputig.eu)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
public class IntroActivity extends AppCompatActivity {

    private AccountManager mAccountManager;
    private String authToken = null;
    private Account mConnectedAccount;
    private TextView initAppView;
    private boolean okInstalled =false;
    private boolean okAccount = false;
    private boolean okData = false;
    private String okInstalledText = "";
    private boolean isAfterSuccessDownload = false;

    private Button button;

    private ProgressDialog dialog;
    private int dataProgress = 0;
    private long dataCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.init_main);
        initAppView = (TextView) findViewById(R.id.init_app_text);
        mAccountManager = AccountManager.get(this);
        Toolbar topToolBar = (Toolbar) findViewById(R.id.init_toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setTitle(R.string.funfi_init_test);

        button = (Button) findViewById(R.id.start_button);
    }

    @Override
    protected void onResume(){
        super.onResume();

        dialog = new ProgressDialog(IntroActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Aktualizácia údajov");
        dialog.setMessage("Chvílu strpenia prosim ... Pre správne stiahnutie prosím udržte obrazovku zasvietenú a aplikáciu neminimalizujte.");
        dialog.setCancelable(false);

        //registerObserver();
        appInitCheck();

    }

    @Override
    protected void onPause(){
        super.onPause();

        dialog.dismiss();
        //unRegisterObserver();
    }

    public void appInitCheck(){
        String installed = checkCOhaveInstallation();
        if (installed!=null){
            okInstalledText = installed;
            okInstalled=true;
            //afterInitCheck();
            //return;
        }else{
            okInstalled = true;
        }

        if (mConnectedAccount == null){
            okAccount=false;
            getTokenForAccountCreateIfNeeded(AccountGeneral.ACCOUNT_TYPE, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS);
            afterInitCheck();
            return;
        }else if (mConnectedAccount!=null && !TextUtils.isEmpty(mConnectedAccount.name) && !TextUtils.isEmpty(authToken)){
            okAccount=true;
        }else{
            okAccount=false;
            afterInitCheck();
            return;
        }


        long pocet = 0;
        Cursor c = getContentResolver().query(WifiContentProvider.WIFI_CONTENT_URI, new String[]{"count(*) as pocet"}, null, null, null);
        if (c != null && !c.isClosed()) {
            c.moveToFirst();
            pocet = c.getLong(0);
            c.close();
        }
        //dataCount = pocet;

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (pocet>=10000 && (cm.getActiveNetworkInfo() == null || !cm.getActiveNetworkInfo().isConnected())) {
            okData=true;
            afterInitCheck();
            return;
        }

        if (cm.getActiveNetworkInfo() == null || !cm.getActiveNetworkInfo().isConnected()) {
            Toast.makeText(this, " Skontrolujte internetove pripojenie !", Toast.LENGTH_LONG).show();
            okData = false;
            afterInitCheck();
            return;
        } else {
            startDownload(); //check for updates
            return;
        }
    }

    public void afterInitCheck(){
        String text = "Minimálne požiadavky: \n";
        if (okInstalled) {
            text += "\nNainštalovaná COhave aplikácia min. verzia 1.2 \t Áno\n";
        }else{
            text += "\n"+okInstalledText+"\n";
        }
        if (okAccount==false){
            text += "\nÚčet COhave \t nebol na zariadení nájdený\n";
        }else{
            text += "\nÚčet COhave \t Áno\n";
        }
        if (okData){
            if (isAfterSuccessDownload == false) {
                text += "\nStiahnté počiatočné údaje \t Áno\n";
            }else{
                text += "\nStiahnté počiatočné údaje \t Prebieha\n";
                text+= "\n\n Informácia: Už teraz môžete pokračovať do FunFi aplikácie, ale neodporúča sa to kvôli" +
                        " hromadnému načítavaniu údajov. Môžete vidieť len pár údajov a tiež môže aplikácia \"sekat\". " +
                        "Počkajte radšej chvíľu. ";
                text+= "\n\n Odporúčanie: Zatvorte a následne otvorte aplikáciu pre načítanie nových dát.";
            }
        }else{
            text+= "\nStiahnté počiatočné údaje \t Nie\n";
        }



        initAppView.setText(text);

        if (okInstalled && okAccount && okData){

            button.setEnabled(true);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            });

            if (isAfterSuccessDownload == false) {
                Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    }


    private String checkCOhaveInstallation(){
        List<PackageInfo> packages = this.getPackageManager().getInstalledPackages(0);
        for (PackageInfo info : packages) {
            if (info.packageName.equalsIgnoreCase("eu.mcomputing.cohave")){
                if (info.versionCode >= 8){
                    return null;
                }else{
                    return "Aktualizujte aplikáciu COhave aspoň na verziu 1.2 pred spustením FunFi";
                }
            }

        }

        return "Nainštalujte si najprv aplikáciu COhave z Google Play.";
    }


    private void getTokenForAccountCreateIfNeeded(String accountType, final String authTokenType) {

        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthTokenByFeatures(accountType, authTokenType, null, this, null, null,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        Bundle bnd = null;
                        try {
                            bnd = future.getResult();
                            authToken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                            if (authToken != null) {
                                String accountName = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                                mConnectedAccount = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);

                                appInitCheck();
                            } else {
                                Logger.stopSensing(getApplicationContext());
                                getTokenForAccountCreateIfNeeded(AccountGeneral.ACCOUNT_TYPE, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS);
                            }

                            MyLog.log(getClass(), ((authToken != null) ? ("SUCCESS! token: " + authToken) : "FAIL"));
                            MyLog.log(getClass(), "GetTokenForAccount Bundle is " + bnd);

                        } catch (Exception e) {
                            e.printStackTrace();
                            MyLog.log(getClass(), e.getMessage());
                            ACRA.getErrorReporter().handleSilentException(e);
                        }
                    }
                }
                , null);
    }

    public void startDownload(){
        Intent service;
        service = new Intent(getApplicationContext(), MyDownloadService.class);
        // Start the service, keeping the device awake while it is launching.
        startService(service);

        okData=true;
        afterInitCheck();

    }

}
