package us.cboyd.android.dicom;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.io.DicomInputStream;

import us.cboyd.android.shared.Internals;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * DICOM InfoFragment
 * 
 * @author Christopher Boyd
 * @version 0.3
 *
 */
public class DcmInfoFragment extends Fragment {
    private static String 		mCurrDir 	 = null;
    private static DicomObject 	mDicomObject = null;
    private static int 			mPosition 	 = 0;
    private static ArrayList<String> mFileList 	 = null;
    private static Button 	mLoadButton;
    private static TextView mArticle;
	
	/**  Array adapter for the tag listing. */
	private ArrayList<String> mTags = new ArrayList<String>();
	private ArrayAdapter<String> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
    	Log.i("cpb","DcmInfoFrag: creation");
    	///Causes issue with landscape view
    	/*if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist.  The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed.  Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.

        	Log.i("cpb","DcmInfoFrag: null");
            return null;
        }*/

        // If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
        	Log.i("cpb","DcmInfoFrag: savedInstance");
            mPosition	= savedInstanceState.getInt(DcmVar.POSITION);
        	mFileList 	= savedInstanceState.getStringArrayList(DcmVar.FILELIST);
            mCurrDir 	= savedInstanceState.getString(DcmVar.CURRDIR);
        }

    	Log.i("cpb","DcmInfoFrag: view");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.dcm_info, container, false);

    	Log.i("cpb","DcmInfoFrag: buttons");
        mLoadButton = (Button) view.findViewById(R.id.loadButton);
        mLoadButton.setEnabled(false);
        mArticle 	= (TextView) view.findViewById(R.id.article);
        mArticle.setText("Test");

    	Log.i("cpb","DcmInfoFrag: return");
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // During startup, check if there are arguments passed to the fragment.
        // onStart is a good place to do this because the layout has already been
        // applied to the fragment at this point so we can safely call the method
        // below that sets the article text.
        Bundle args = getArguments();
        if (args != null) {
            // Set article based on argument passed in
            updateDicomInfo(args.getInt(DcmVar.POSITION),
            		args.getStringArrayList(DcmVar.FILELIST), args.getString(DcmVar.CURRDIR));
        } else if ((mCurrDir != null) && (mFileList != null) && (mPosition != 0)) {
            // Set article based on saved instance state defined during onCreateView
            updateDicomInfo(mPosition, mFileList, mCurrDir);
        }
    }
    
    public String getDicomFile() {
    	Log.i("cpb", "File DIR: " + mCurrDir);
    	Log.i("cpb", "File ID: " + mPosition + " FileList count: " + mFileList.size());
    	return mCurrDir + '/' + mFileList.get(mPosition);
    }
    
    public List<String> getFileList() {
    	return mFileList;
    }

    public void updateDicomInfo(int position, ArrayList<String> dirList, String currDir) {
    	mPosition 	= position;
    	mFileList 	= dirList;
    	mCurrDir 	= currDir;
    	updateDicomInfo();
    }

    public void updateDicomInfo() {
    	mDicomObject = null;
    	if ((mCurrDir != null) && (mFileList != null)
    			&& (mPosition >= 0) && (mPosition < mFileList.size())) {
	    	try {
				// Read in the DicomObject
				DicomInputStream dis = new DicomInputStream(new FileInputStream(getDicomFile()));
				//dicomObject = dis.readDicomObject();
				mDicomObject = dis.readFileMetaInformation();
				dis.close();
				
				// Get the SOP Class element
				DicomElement de = mDicomObject.get(Tag.MediaStorageSOPClassUID);
				String SOPClass = "";
				if (de != null)
					SOPClass = de.getString(new SpecificCharacterSet(""), true);
				else
					SOPClass = "null";
				Log.i("cpb", "SOP Class: " + SOPClass);
				
				if (SOPClass.equals(UID.MediaStorageDirectoryStorage)) {
					mLoadButton.setEnabled(false);
				} else {
					mLoadButton.setEnabled(true);
				}
				
				// Create an array adapter for the list view, using the files array
		        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.file_list_item_2, android.R.id.text1, mTags) {
		        	  @Override
		        	  public View getView(int position, View convertView, ViewGroup parent) {
		        	    View view = super.getView(position, convertView, parent);
		        	    ImageView icon = (ImageView) view.findViewById(R.id.icon);
		        	    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
		        	    TextView text2 = (TextView) view.findViewById(android.R.id.text2);
		        	    
		        	    text1.setText(Internals.getString("dcm_" + mTags.get(position), view));
		        	    text2.setText(mDicomObject.getString(Integer.parseInt(mTags.get(position))));
		        	  }
		        	};
				//mAdapter = new ArrayAdapter<String>(getActivity(), layout, mDirList);
				setListAdapter(mAdapter);
				
			} catch (Exception ex) {
	            Resources res = getResources();
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(res.getString(R.string.err_mesg_read) + mFileList.get(mPosition)
						+ "\n\n" + ex.getMessage())
					   .setTitle(res.getString(R.string.err_title_read))
				       .setCancelable(false)
				       .setPositiveButton(res.getString(R.string.err_ok), new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                // Do nothing
				           }
				       });
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
				
			}
    	} else {
    		mLoadButton.setEnabled(false);
        }
    	
        if (mDicomObject == null) {
            mArticle.setText("NULL");
    		mLoadButton.setEnabled(false);
        } else {
        	mArticle.setText("Example");
        }

    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current article selection in case we need to recreate the fragment
        outState.putInt(DcmVar.POSITION, mPosition);
        outState.putStringArrayList(DcmVar.FILELIST, mFileList);
        outState.putString(DcmVar.FILELIST, mCurrDir);
    }
}