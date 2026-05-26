package com.krishna.crimedetection.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.EmergencyContact;
import com.krishna.crimedetection.models.EmergencyContactDao;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyContactViewModel extends AndroidViewModel {
    private final EmergencyContactDao contactDao;
    private final ExecutorService executorService;
    private final MutableLiveData<List<EmergencyContact>> _emergencyContacts = new MutableLiveData<>();
    private final MutableLiveData<EmergencyContact> _primaryContact = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public EmergencyContactViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        contactDao = db.emergencyContactDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<EmergencyContact>> getEmergencyContacts() { return _emergencyContacts; }
    public LiveData<EmergencyContact> getPrimaryContact() { return _primaryContact; }
    public LiveData<Boolean> isLoading() { return _isLoading; }

    public void loadContacts(int userId) {
        _isLoading.setValue(true);
        executorService.execute(() -> {
            List<EmergencyContact> contacts = contactDao.getAllContacts(userId);
            EmergencyContact primary = contactDao.getPrimaryContact(userId);
            _emergencyContacts.postValue(contacts);
            _primaryContact.postValue(primary);
            _isLoading.postValue(false);
        });
    }

    public void addContact(EmergencyContact contact) {
        executorService.execute(() -> {
            contactDao.insertContact(contact);
            loadContacts(contact.getUserId());
        });
    }

    public void updateContact(EmergencyContact contact) {
        executorService.execute(() -> {
            contactDao.updateContact(contact);
            loadContacts(contact.getUserId());
        });
    }

    public void deleteContact(int userId, int contactId) {
        executorService.execute(() -> {
            contactDao.deleteContactById(userId, contactId);
            loadContacts(userId);
        });
    }

    public void setPrimaryContact(int userId, int contactId) {
        executorService.execute(() -> {
            List<EmergencyContact> contacts = contactDao.getAllContacts(userId);
            for (EmergencyContact c : contacts) {
                c.setPrimary(c.getId() == contactId);
                contactDao.updateContact(c);
            }
            loadContacts(userId);
        });
    }

    public void toggleContactActive(int userId, int contactId, boolean active) {
        executorService.execute(() -> {
            List<EmergencyContact> contacts = contactDao.getAllContacts(userId);
            for (EmergencyContact c : contacts) {
                if (c.getId() == contactId) {
                    c.setActive(active);
                    contactDao.updateContact(c);
                    break;
                }
            }
            loadContacts(userId);
        });
    }
}
