package com.esri.arcgis.sample;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.esri.core.tasks.na.CostAttribute;
import com.esri.core.tasks.na.NetworkDescription;
import com.esri.core.tasks.na.RestrictionAttribute;
import com.esri.core.tasks.na.RouteParameters;

public class SettingsComponent extends PreferenceFragment {

  public static final String IMPEDANCE_PREF_KEY = "pref_impedance";
  
  public static final String RESTRICTIONS_PREF_KEY = "pref_restrictions";
  
  public static final String DIRECTIONS_LANGUAGE_PREF_KEY = "pref_directions_language";
  
  public static final String DIRECTIONS_PREF_KEY = "pref_directions";
  
  public static final String TSP_PREF_KEY = "pref_tsp";
  
  public static final String TSP_FIRST_PREF_KEY = "pref_tsp_first";
  
  public static final String TSP_LAST_PREF_KEY = "pref_tsp_last";
  
  private static final String ND_RESTRICTIONS_KEY = "nd_restrictions";
  
  private static final String RP_RESTRICTIONS_KEY = "rp_restrictions";
  
  private static final String ND_COST_ATTRIBUTES_KEY = "nd_cost_attributes";
  
  private static final String RP_IMEDANCE_KEY = "rp_impedance";
  
  private static final String RP_TSP_KEY = "rp_tsp";  
  
  private static final String RP_TSP_FIRST_KEY = "rp_tsp_first";
  
  private static final String RP_TSP_LAST_KEY = "rp_tsp_last";
  
  private static final String ND_DIRECTIONS_LANGUAGES_KEY = "nd_directions_languges";
  
  private static final String RP_DIRECTIONS_LANGUAGE_KEY = "rp_direcitons_language";
  
  private static final String RP_DIRECTIONS_KEY = "rp_directions";
  
  /**
   * Default constructor.
   */
  public SettingsComponent() {
    
  }
    
  /**
   * Create a new settings fragment with a bundle. The bundle should be 
   * created using {@link SettingsComponent#createBundle(NetworkDescription, RouteParameters)}.
   * 
   * @param args The bundle containing default and current settings information.
   * @return A settings Fragment.
   */
  public static SettingsComponent newInstance(Bundle args) {
    SettingsComponent fragment = new SettingsComponent();
    fragment.setArguments(args);
    return fragment;
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view =  super.onCreateView(inflater, container, savedInstanceState);
    view.setBackgroundColor(Color.WHITE);
    return view;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);  
    adjustSettings(getArguments());
  }    
  
  /**
   * Given a network description and route parameters, create an appropriate bundle
   * for all relevant settings.
   * 
   * @param description A network description, can be null.
   * @param routeParams A route parameters object, can be null.
   * @return
   */
  public static Bundle createBundle(NetworkDescription description, RouteParameters routeParams) {
    
    Bundle args = new Bundle();
    
    // Assign all route parameters relative values.
    if (routeParams != null) {
      args.putString(RP_IMEDANCE_KEY, routeParams.getImpedanceAttributeName());
      args.putStringArray(RP_RESTRICTIONS_KEY, routeParams.getRestrictionAttributeNames());
      args.putBoolean(RP_TSP_KEY, routeParams.getFindBestSequence());
      args.putBoolean(RP_TSP_FIRST_KEY, routeParams.getPreserveFirstStop());
      args.putBoolean(RP_TSP_LAST_KEY, routeParams.getPreserveLastStop());
      args.putString(RP_DIRECTIONS_LANGUAGE_KEY, routeParams.getDirectionsLanguage());
      args.putBoolean(RP_DIRECTIONS_KEY, routeParams.isReturnDirections());
    }
    
    // Assign available restrictions, cost attributes, and directions languages.
    if (description != null) {
      
      List<RestrictionAttribute> restrictions = description.getRestrictionAttributes();
      List<CostAttribute> costs = description.getCostAttributes();
      
      String[] restrictionNames = new String[restrictions.size()];
      String[] costNames = new String[costs.size()];
      
      for (int i = 0; i < restrictions.size(); i++)
        restrictionNames[i] = restrictions.get(i).getName();
      
      for (int i = 0; i < costs.size(); i++)
        costNames[i] = costs.get(i).getName();
      
      args.putStringArray(ND_RESTRICTIONS_KEY, restrictionNames);
      args.putStringArray(ND_COST_ATTRIBUTES_KEY, costNames);
      
      List<String> languages = description.getSupportedDirectionsLanguages();
      args.putStringArray(ND_DIRECTIONS_LANGUAGES_KEY, languages.toArray(new String[languages.size()])); 
    }
    
    return args;      
  }
  
  /**
   * As most of our settings are dynamic based on the network description and the 
   * current routing parameters, we adjust the preference after they are inflated from
   * the layout.
   * 
   * @param bundle A bundle with current parameters, can be null.
   */
  private void adjustSettings(Bundle bundle) {      
    
    if (bundle == null)
      return;
    
    // Impedance preference
    ListPreference impedancePref = (ListPreference) findPreference(IMPEDANCE_PREF_KEY);
    String[] costAttributes = bundle.getStringArray(ND_COST_ATTRIBUTES_KEY);
    String impedance = bundle.getString(RP_IMEDANCE_KEY);
    
    if (impedance == null || costAttributes == null)
      impedancePref.setEnabled(false);
    else {
      impedancePref.setEntries(costAttributes);
      impedancePref.setEntryValues(costAttributes);
      impedancePref.setValue(impedance);
    }
    
    // Directions enabled preference
    CheckBoxPreference dirPref = (CheckBoxPreference) findPreference(DIRECTIONS_PREF_KEY);
    dirPref.setEnabled(bundle.containsKey(RP_DIRECTIONS_KEY));
    dirPref.setChecked(bundle.getBoolean(RP_DIRECTIONS_KEY, false));
    
    // Directions language preference
    ListPreference dirLanguagePref = (ListPreference) findPreference(DIRECTIONS_LANGUAGE_PREF_KEY);
    String[] supportedLanguages = bundle.getStringArray(ND_DIRECTIONS_LANGUAGES_KEY);
    String currentLanguage = bundle.getString(RP_DIRECTIONS_LANGUAGE_KEY);
    
    if (supportedLanguages == null || currentLanguage == null)
      dirLanguagePref.setEnabled(false);
    else {
      dirLanguagePref.setEntries(supportedLanguages);
      dirLanguagePref.setEntryValues(supportedLanguages);
      dirLanguagePref.setValue(currentLanguage);
    }
    
    // Restrictions preferences
    MultiSelectListPreference restrictionsPref = (MultiSelectListPreference) findPreference(RESTRICTIONS_PREF_KEY);
    String[] potentialRestrictions = bundle.getStringArray(ND_RESTRICTIONS_KEY);
    String[] activeRestrictions = bundle.getStringArray(RP_RESTRICTIONS_KEY);
    
    if (potentialRestrictions == null || activeRestrictions == null)
      restrictionsPref.setEnabled(false);
    else {
      restrictionsPref.setEntries(potentialRestrictions);
      restrictionsPref.setEntryValues(potentialRestrictions);
      restrictionsPref.setValues(new HashSet<String>(Arrays.asList(activeRestrictions)));
    }
    
    // Optimization
    CheckBoxPreference tspPref = (CheckBoxPreference) findPreference(TSP_PREF_KEY);
    tspPref.setEnabled(bundle.containsKey(RP_TSP_KEY));
    tspPref.setChecked(bundle.getBoolean(RP_TSP_KEY, false));
    
    // Preserve first stop
    tspPref = (CheckBoxPreference) findPreference(TSP_FIRST_PREF_KEY);
    tspPref.setEnabled(bundle.containsKey(RP_TSP_FIRST_KEY));
    tspPref.setChecked(bundle.getBoolean(RP_TSP_FIRST_KEY, true));
    
    // Preserve last stop
    tspPref = (CheckBoxPreference) findPreference(TSP_LAST_PREF_KEY);
    tspPref.setEnabled(bundle.containsKey(RP_TSP_LAST_KEY));
    tspPref.setChecked(bundle.getBoolean(RP_TSP_LAST_KEY, true));    
  }
}
