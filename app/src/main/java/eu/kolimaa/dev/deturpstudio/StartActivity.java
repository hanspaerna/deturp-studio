package eu.kolimaa.dev.deturpstudio;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

public class StartActivity extends Activity implements EditDialogFragment.TrackOperator,
        AdapterView.OnItemLongClickListener {

    ArrayList<Track> playListTracks;

    TrackListAdapter trackListAdapter;

    EditDialogFragment newTrackDialogFragment, editTrackDialogFragment;

    FragmentManager fm = getFragmentManager();

    private Intent getExplicitMusicServiceIntent() {
        Intent service = new Intent(this, MusicService.class);
        return service;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);

        if (playListTracks == null) {
            playListTracks = new ArrayList<>();
        }

        MusicService.getServiceBus().register(this);
        startService(getExplicitMusicServiceIntent());

        trackListAdapter = new TrackListAdapter(getApplicationContext(), playListTracks);

        final ListView playlistView = (ListView) findViewById(R.id.playlist);
        final ToggleButton playToggleButton = (ToggleButton) findViewById(R.id.toggle_button_play);
        final Button stopButton = (Button) findViewById(R.id.button_stop);

        playToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MusicService.getInstance().play();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicService.getInstance().stop();
            }
        });

        playlistView.setAdapter(trackListAdapter); //setting the adapter for playlist view
        playlistView.setOnItemLongClickListener(this);

    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {


        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {

        Track currentTrack = playListTracks.get(position);

        editTrackDialogFragment = EditDialogFragment.newInstance(false);

        editTrackDialogFragment.setTrackPosition(position);
        editTrackDialogFragment.setCurrentName(currentTrack.getTrackName());
        editTrackDialogFragment.setCurrentFilePath(currentTrack.getTrackPath());

        editTrackDialogFragment.show(fm, "editTrackDialog");
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_start, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.action_quitbutton:
                AppHelper.killApplication();
            case R.id.action_add:
                if (newTrackDialogFragment == null) {
                    newTrackDialogFragment = EditDialogFragment.newInstance(true);
                }
                newTrackDialogFragment.show(fm, "newTrackDialog");

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateNewTrack(String name, Uri path) {
        Track track = new Track(name, path, getApplicationContext());
        playListTracks.add(track);
        newTrackDialogFragment = null;

        trackListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEditExistingTrack(String newName, Uri newPath, int position) {
        Track currentTrack = playListTracks.get(position);

        currentTrack.setTrackName(newName);
        currentTrack.setTrackPath(newPath);

        trackListAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onServiceStarted(ServiceEvent event) {
        MusicService.getInstance().setTracks(playListTracks);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            Uri uri = data.getData();
            Log.e("ONACTIVITYRESULT", uri.toString());
            if (newTrackDialogFragment != null) {
                newTrackDialogFragment.setCurrentFilePath(uri);
            } else {
                editTrackDialogFragment.setCurrentFilePath(uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public interface MusicPlayerController {

        public void play();

        public void stop();

        public void setTracks(ArrayList<Track> tracks);

    }

}