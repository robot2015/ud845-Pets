/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Tag for the log messages.
    private static final String LOG_TAG = EditorActivity.class.getSimpleName();

    // Identifier for the pet data loader.
    private static final int EXISTING_PET_LOADER = 0;

    // Content URI for the existing pet (null if it's a new pet).
    private Uri mCurrentPetUri;

    // EditText field to enter the pet's name.
    private EditText mNameEditText;

    // EditText field to enter the pet's breed.
    private EditText mBreedEditText;

    // EditText field to enter the pet's weight.
    private EditText mWeightEditText;

    // EditText field to enter the pet's gender.
    private Spinner mGenderSpinner;

    // Content URI for the existing pet (null if it's a new pet).
    private Uri currentPetUri;

    // Get name of pet to use elsewhere.
    private String currentPetName;

    // Check if the pet has changed.
    private boolean petHasChanged = false;

    /**
     * Gender of the pet. The possible values are:
     * 0 for unknown gender, 1 for male, 2 for female.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;

    // Get user input from editor and save pet into database.
    private void savePet() {
        Log.v(LOG_TAG, "savePet()");

        // Get user input.
        String nameString = mNameEditText.getText().toString().trim();
        String breedString = mBreedEditText.getText().toString().trim();
        String weightString = mWeightEditText.getText().toString().trim();

        // Check if all fields are blank.  If yes, return without doing anything.
        if (currentPetUri == null
                && TextUtils.isEmpty(nameString)
                && TextUtils.isEmpty(breedString)
                && TextUtils.isEmpty(weightString)
                && mGender == PetEntry.GENDER_UNKNOWN) {
            return;
        }

        // If the weight is not provided by the user use 0 by default.
        int weightInt;
        if (TextUtils.isEmpty(weightString)) {
            weightInt = 0;
        } else {
            weightInt = Integer.parseInt(weightString);
        }

        // Create a ContentValues object where pet attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(PetEntry.COLUMN_PET_NAME, nameString);
        values.put(PetEntry.COLUMN_PET_BREED, breedString);
        values.put(PetEntry.COLUMN_PET_GENDER, mGender);
        values.put(PetEntry.COLUMN_PET_WEIGHT, weightInt);

        // Determine if this is a new or existing pet by checking if mCurrentPetUri is null or not
        if (currentPetUri == null) {
            // This is a NEW pet, so insert a new pet into the provider,
            // returning the content URI for the new pet.
            Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_pet_failed, nameString),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_pet_successful, nameString),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING pet, so update the pet with content URI.
            int rowsAffected = getContentResolver().update(currentPetUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_pet_failed, nameString),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_pet_successful, nameString),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate(Bundle savedInstanceState)");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Get URI if activity was initiated from clicking pet.
        Intent intent = getIntent();
        currentPetUri = intent.getData();

        // Change the activity title to Edit Pet, if necessary.
        if (currentPetUri == null) {
            this.setTitle(R.string.editor_activity_title_new_pet);
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            invalidateOptionsMenu();
        } else {
            this.setTitle(R.string.editor_activity_title_edit_pet);

            // Initialize a loader to read the pet data from the database.
            getSupportLoaderManager().initLoader(EXISTING_PET_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = findViewById(R.id.edit_pet_name);
        mBreedEditText = findViewById(R.id.edit_pet_breed);
        mWeightEditText = findViewById(R.id.edit_pet_weight);
        mGenderSpinner = findViewById(R.id.spinner_gender);

        setupSpinner();

        // Set touch listeners.
        mNameEditText.setOnTouchListener(touchListener);
        mBreedEditText.setOnTouchListener(touchListener);
        mWeightEditText.setOnTouchListener(touchListener);
        mGenderSpinner.setOnTouchListener(touchListener);
    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        Log.v(LOG_TAG, "setupSpinner()");
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE; // Female
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = 0; // Unknown
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(LOG_TAG, "onCreateOptionsMenu(Menu menu)");
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    //  This method is called after invalidateOptionsMenu(), so that the menu can be updated.
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (currentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(LOG_TAG, "onOptionsItemSelected(MenuItem item)");
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save pet to database
                savePet();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion.
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                Log.v(LOG_TAG, "onOptionsItemSelected: case android.R.id.home");

                // If the pet hasn't changed, continue with navigating up to parent activity.
                if (!petHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                Log.v(LOG_TAG, "onOptionsItemSelected: DialogInterface.OnClickListener discardButtonClickListener");

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                Log.v(LOG_TAG, "onOptionsItemSelected: showUnsavedChangesDialog(discardButtonClickListener)");
                return true;
        }
        Log.v(LOG_TAG, "onOptionsItemSelected: return super.onOptionsItemSelected(item)");
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        Log.v(LOG_TAG, "Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args)");
        // Define a projection that contains all columns from the pet table.
        String[] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_GENDER,
                PetEntry.COLUMN_PET_WEIGHT};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                currentPetUri,         // Query the content URI for the current pet
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        Log.v(LOG_TAG, "onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor)");
        // Bail early if the cursor is null or there is less than 1 row in the cursor.
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it.
        if (cursor.moveToFirst()) {
            // Find the columns of pet attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME);
            int breedColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_BREED);
            int genderColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_GENDER);
            int weightColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String breed = cursor.getString(breedColumnIndex);
            int gender = cursor.getInt(genderColumnIndex);
            int weight = cursor.getInt(weightColumnIndex);

            // Save pet name as class variable.
            currentPetName = name;

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mBreedEditText.setText(breed);
            mWeightEditText.setText(Integer.toString(weight));

            // Set gender dropdown spinner.
            switch (gender) {
                case PetEntry.GENDER_MALE:
                    mGenderSpinner.setSelection(1);
                    break;
                case PetEntry.GENDER_FEMALE:
                    mGenderSpinner.setSelection(2);
                    break;
                default:
                    mGenderSpinner.setSelection(0);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        Log.v(LOG_TAG, "onLoaderReset(@NonNull Loader<Cursor> loader)");
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mBreedEditText.setText("");
        mWeightEditText.setText("");
        mGenderSpinner.setSelection(0); // Select "Unknown" gender
    }

    // Listen for whether changes were made.
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Log.v(LOG_TAG, "onTouch(View view, MotionEvent motionEvent)");
            petHasChanged = true;
            return false;
        }
    };

    // Make a method for creating a “Discard changes” dialog.
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {

        Log.v(LOG_TAG, "showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener)");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Log.v(LOG_TAG, "showUnsavedChangesDialog: builder");
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        Log.v(LOG_TAG, "showUnsavedChangesDialog: setMessage");
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        Log.v(LOG_TAG, "showUnsavedChangesDialog: setPositiveButton");
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog.
                Log.v(LOG_TAG, "showUnsavedChangesDialog: setNegativeButton");
                if (dialog != null) {
                    Log.v(LOG_TAG, "dialog.dismiss()");
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog.
        AlertDialog alertDialog = builder.create();
        Log.v(LOG_TAG, "showUnsavedChangesDialog: alertDialog");
        alertDialog.show();
        Log.v(LOG_TAG, "showUnsavedChangesDialog: alertDialog.show()");
    }

    // Hook up the back button.
    @Override
    public void onBackPressed() {
        Log.v(LOG_TAG, "onBackPressed()");

        // If the pet hasn't changed, continue with handling back button press.
        if (!petHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes.
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    // Prompt the user to confirm that they want to delete this pet.
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.delete_dialog_msg, currentPetName));
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Perform the deletion of the pet in the database.
    private void deletePet() {
        // Only perform the delete if this is an existing pet.
        if (currentPetUri != null) {

            // Call the ContentResolver to delete the pet at the given content URI.
            int rowsDeleted = getContentResolver().delete(currentPetUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_pet_failed, currentPetName),
                        Toast.LENGTH_LONG).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_pet_successful, currentPetName),
                        Toast.LENGTH_LONG).show();
            }
        }

        // Close the activity
        finish();
    }
}