/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.validation.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.validation.internal.delegates.ValidatorDelegateDescriptor;
import org.eclipse.wst.validation.internal.plugin.ValidationPlugin;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This class represents the Project Preferences as set on the Project's Validation Properties page.
 */
public class ProjectConfiguration extends ValidationConfiguration {
	
	/** false - If the user has never set a preference before, this is the override default */
	static final boolean PRJ_OVERRIDEGLOBAL_DEFAULT = false;
	
	/** default_value - the configuration is using the defaults. */
	private static final String DefaultValue = "default_value"; //$NON-NLS-1$
	
	private boolean _doesProjectOverride = getDoesProjectOverrideDefault();

	/**
	 * This constructor should be used in all cases except for the Properties page's values.
	 */
	protected ProjectConfiguration(IProject project) throws InvocationTargetException {
		// The extractProjectValidators method extracts just this project's validators from the
		// global list.
		super(project, extractProjectValidators(convertToArray(ValidationRegistryReader.getReader().getAllValidators()), project));

		// Can't put the call to load() and passivate() in the ValidationConfiguration constructor due
		// to the order of initialization.
		//    1. First the ValidationConfiguration constructor is called, and that loads the stored values.
		//    2. Then this class's <init> method is called, and that initializes the "override" field
		//       to the default, which may be different than the stored value.
	}

	/**
	 * This constructor is provided only for the Properties page, so that the page can store values
	 * without persisting them (i.e., if the user presses Cancel then nothing needs to be done.)
	 * 
	 * @deprecated
	 */
	public ProjectConfiguration(ProjectConfiguration original) throws InvocationTargetException {
		super(original.getResource());
		original.copyTo(this);
	}

	/**
	 * Return the ValidationConfiguration to use, whether global or project.
	 */
	public boolean useGlobalPreference() {
		try {
			GlobalConfiguration gp = ConfigurationManager.getManager().getGlobalConfiguration();
			if (gp == null) {
				return false;
			}

			if (!gp.canProjectsOverride()) {
				// If project's can't override the global, use the global
				return true;
			}

			// If the project overrides, then don't use the global.
			// If the project does not override, use the global.
			return !_doesProjectOverride;
		} catch (InvocationTargetException e) {
			ValidationPlugin.getPlugin().handleException(e);
			return false;
		}
	}
	
	public boolean getDoesProjectOverride(){
		return _doesProjectOverride;
	}

	public boolean doesProjectOverride() {
		// If the global preference doesn't allow projects to override, it doesn't matter what the
		// value of _doesProjectOverride is.
		return !useGlobalPreference();
	}

	public void setDoesProjectOverride(boolean does) {
		_doesProjectOverride = does;
	}

	/**
	 * If the preferences should be used then the preference settings are returned; otherwise return
	 * the project settings.
	 */
	public ValidatorMetaData[] getEnabledValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getEnabledValidators(), getResource());
		}
		return super.getEnabledValidators();
	}
	
	public ValidatorMetaData[] getManualEnabledValidators() throws InvocationTargetException  {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getManualEnabledValidators(), getResource());
		}
		return super.getManualEnabledValidators();
	}
	
	public ValidatorMetaData[] getBuildEnabledValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getBuildEnabledValidators(), getResource());
		}
		return super.getBuildEnabledValidators();
	}
	

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.preference.ValidationConfiguration#getDisabledValidators()
	 */
	public ValidatorMetaData[] getDisabledValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getDisabledValidators(), getResource());
		}
		return super.getDisabledValidators();
	}

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.preference.ValidationConfiguration#getValidators()
	 */
	public ValidatorMetaData[] getValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getValidators(), getResource());
		}
		return super.getValidators();
	}

  public Map<String,String> getDelegatingValidators() throws InvocationTargetException {
    if (useGlobalPreference()) {
      return ConfigurationManager.getManager().getGlobalConfiguration().getDelegatingValidators();
    }
    return super.getDelegatingValidators();
  }

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.attribute.ValidationConfiguration#getEnabledIncrementalValidators(boolean)
	 */
	public ValidatorMetaData[] getEnabledIncrementalValidators(boolean incremental) throws InvocationTargetException {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getEnabledIncrementalValidators(incremental), getResource());
		}
		return super.getEnabledIncrementalValidators(incremental);
	}

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.preference.ValidationConfiguration#isEnabled(ValidatorMetaData)
	 */
	public boolean isEnabled(ValidatorMetaData vmd) throws InvocationTargetException {
		if (useGlobalPreference()) {
			return ConfigurationManager.getManager().getGlobalConfiguration().isEnabled(vmd);
		}
		return super.isEnabled(vmd);
	}

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.preference.ValidationConfiguration#numberOfDisabledValidators()
	 */
	public int numberOfDisabledValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return ConfigurationManager.getManager().getGlobalConfiguration().numberOfDisabledValidators();
		}
		return super.numberOfDisabledValidators();
	}

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.preference.ValidationConfiguration#numberOfEnabledIncrementalValidators()
	 */
	public int numberOfEnabledIncrementalValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return ConfigurationManager.getManager().getGlobalConfiguration().numberOfEnabledIncrementalValidators();
		}
		return super.numberOfEnabledIncrementalValidators();
	}

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.preference.ValidationConfiguration#numberOfEnabledValidators()
	 */
	public int numberOfEnabledValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return ConfigurationManager.getManager().getGlobalConfiguration().numberOfEnabledValidators();
		}
		return super.numberOfEnabledValidators();
	}

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.preference.ValidationConfiguration#numberOfIncrementalValidators()
	 */
	public int numberOfIncrementalValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return ConfigurationManager.getManager().getGlobalConfiguration().numberOfIncrementalValidators();
		}
		return super.numberOfIncrementalValidators();
	}

	/**
	 * @see org.eclipse.wst.validation.internal.operations.internal.preference.ValidationConfiguration#numberOfValidators()
	 */
	public int numberOfValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return ConfigurationManager.getManager().getGlobalConfiguration().numberOfValidators();
		}
		return super.numberOfValidators();
	}

	/**
	 * This method could be called with the project's values, or with the global preference values.
	 * Validators that are not configured on this project will be ignored.
	 */
	public void setEnabledValidators(ValidatorMetaData[] vmds) {
		super.setEnabledValidators(extractProjectValidators(vmds, getResource()));
	}

	/**
	 * This method could be called with the project's values, or with the global preference values.
	 * Validators that are not configured on this project will be ignored.
	 */
	public void setValidators(ValidatorMetaData[] vmds) {
		super.setValidators(extractProjectValidators(vmds, getResource()));
	}

	/**
	 * Given a set of validators, usually the global preference set, change the set so that it
	 * contains only the validators configured on this project.
	 */
	private static ValidatorMetaData[] extractProjectValidators(ValidatorMetaData[] vmds, IResource resource) {
		ValidationRegistryReader reader = ValidationRegistryReader.getReader();
		int length = (vmds == null) ? 0 : vmds.length;
		ValidatorMetaData[] temp = new ValidatorMetaData[length];
		if (length == 0) {
			return temp;
		}

		int count = 0;
		IProject project = (IProject) resource;
		for (int i = 0; i < vmds.length; i++) {
			ValidatorMetaData vmd = vmds[i];
			if (reader.isConfiguredOnProject(vmd, project)) {
				temp[count++] = vmd;
			}
		}

		ValidatorMetaData[] result = new ValidatorMetaData[count];
		System.arraycopy(temp, 0, result, 0, count);
		temp = null;

		return result;
	}

	public void resetToDefault() throws InvocationTargetException {
		// The default values of the project is whatever the preference values are
		GlobalConfiguration gp = ConfigurationManager.getManager().getGlobalConfiguration();

		setEnabledManualValidators(gp.getManualEnabledValidators());
		setEnabledBuildValidators(gp.getBuildEnabledValidators());
		setDelegatingValidators(gp.getDelegatingValidators());
		
		// except for this field, which is unique to the project preferences
		setDoesProjectOverride(getDoesProjectOverrideDefault());
	}

	public void resetToDefaultForProjectDescriptionChange() throws InvocationTargetException {
		// The default values of the project is whatever the preference values are
		GlobalConfiguration gp = ConfigurationManager.getManager().getGlobalConfiguration();
		setEnabledManualValidators(gp.getManualEnabledValidators());
		setEnabledBuildValidators(gp.getBuildEnabledValidators());
		setDelegatingValidators(gp.getDelegatingValidators());
	}

	/**
	 * The project's nature has changed, so recalculate the validators that are configured on the
	 * project, and reset the values of the project to the default.
	 * 
	 * @deprecated this method doesn't do anything
	 */
	public void resetProjectNature() {
		/*
		 * We do not want to perform the resetting the of the validators as the nature never gets
		 * reset due to change in the project references - VKB GlobalConfiguration gp =
		 * ConfigurationManager.getManager().getGlobalConfiguration();
		 * setValidators(gp.getValidators()); // Reset the validators that are configured on the
		 * project (the ProjectConfiguration automatically saves only the validators that are
		 * configured on the project). resetToDefault(); // Given that the project is "new", reset
		 * its values to the Preferences.
		 */
	}

	/**
	 * This method exists only for migration purposes. The project marker must be deleted after
	 * migration is complete.
	 */
	protected IMarker[] getMarker() {
		try {
			// First try to find the 4.03 project marker.
			IMarker[] allMarkers = getResource().findMarkers(ConfigurationConstants.PRJ_MARKER_403, false, IResource.DEPTH_ZERO);

			// If it doesn't exist, then this might be a 5.0 project marker.
			if ((allMarkers == null) || (allMarkers.length == 0)) {
				allMarkers = getResource().findMarkers(ConfigurationConstants.PRJ_MARKER, false, IResource.DEPTH_ZERO);
			}

			// There should be only one projectmarker.
			if (allMarkers.length == 1) {
				return allMarkers;
			}
			// Job is done. Nothing to migrate.
			return null;
		} catch (CoreException e) {
			ValidationPlugin.getPlugin().handleException(e);
			return null;
		}
	}

	protected void load(IMarker[] marker) {
		// 4.03 project preferences are different from the current in the following ways:
		//    1. Only preferences that could be set were the enabled validators and the auto-validate
		// option.
		//    2. The preferences were stored in an IMarker instead of a PersistentProperty.
		// The 5.0 project settings were stored in an IMarker, and the current settings are stored
		// in a PersistentProperty.
		// A 5.0 project could have a null validation marker if the validation page was never
		// opened on it, and if validation was never run.
		try {
			if (marker == null) {
				// Assume default values
				resetToDefault();
				return;
			}

			IMarker prjMarker = marker[0]; // getProjectMarker() has already checked that there's a
			// marker in the array
//			GlobalConfiguration gp = ConfigurationManager.getManager().getGlobalConfiguration();

//			String enabledValStr = (String) getValue(prjMarker, ConfigurationConstants.ENABLED_VALIDATORS);
//			ValidatorMetaData[] enabledVal = null;
//			if (enabledValStr == null) {
//				enabledVal = gp.getEnabledValidators();
//			} else {
//				enabledVal = getStringAsEnabledElementsArray(enabledValStr);
//			}
//			setEnabledValidators(enabledVal);
			
			ValidatorMetaData[] enabledManaualVal = null;
			ValidatorMetaData[] enabledBuildVal = null;
			String enabledManualValStr = (String) getValue(prjMarker, ConfigurationConstants.ENABLED_MANUAL_VALIDATORS);
			String enabledBuildValStr = (String) getValue(prjMarker, ConfigurationConstants.ENABLED_BUILD_VALIDATORS);
//			ValidatorMetaData[] enabledVal = null;
//			if (enabledManualValStr.equals(null) || enabledBuildValStr.equals(null)) {
//				enabledVal = gp.getEnabledValidators();
//			} else {
			if (!enabledManualValStr.equals(null) && !enabledBuildValStr.equals(null)) {
				enabledManaualVal = getStringAsEnabledElementsArray(enabledManualValStr);
				setEnabledManualValidators(enabledManaualVal);
				enabledBuildVal = getStringAsEnabledElementsArray(enabledManualValStr);
				setEnabledBuildValidators(enabledBuildVal);
			}
			
			String version = loadVersion(marker); // In 4.03, every project had its own validators &
			// auto-validate settings.
			Boolean boolVal = (Boolean) getValue(prjMarker, ConfigurationConstants.PRJ_OVERRIDEGLOBAL);
			if ((boolVal == null) && (version.equals(ConfigurationConstants.VERSION4_03))) {
				// Different default for 4.03. In 4.03, all projects overrode the global, because
				// the
				// global preferences didn't exist.
				setDoesProjectOverride(true);
			} else if (boolVal == null) {
				setDoesProjectOverride(getDoesProjectOverrideDefault());
			} else {
				setDoesProjectOverride(boolVal.booleanValue());
			}

			getResource().getWorkspace().deleteMarkers(marker);
		} catch (CoreException e) {
			ValidationPlugin.getPlugin().handleException(e);
		} catch (InvocationTargetException e) {
			ValidationPlugin.getPlugin().handleException(e);
		}
	}

	protected void copyTo(ProjectConfiguration prjp) throws InvocationTargetException {
		super.copyTo(prjp);

		// Need to have a distinct method for this child class (i.e., the parameter
		// is not a ValidationConfiguration) because if this initialization is
		// called as part of ValidationConfiguration's constructor, then the value of
		// this field is overwritten. Fields of this class are initialized to the
		// default after the ValidationConfiguration parent is created.
		prjp.setDoesProjectOverride(doesProjectOverride());
	}

	public static boolean getDoesProjectOverrideDefault() {
		return PRJ_OVERRIDEGLOBAL_DEFAULT;
	}

	/**
	 * Return true if the enabled validators have not changed since this ValidationConfiguration was
	 * constructed, false otherwise. (This method is needed for the Properties and Preference pages;
	 * if the list of validators hasn't changed, then there is no need to update the task list;
	 * updating the task list is a costly operation.)
	 * 
	 * The "allow" parameter represents whether or not the global "allow projects to override" has
	 * been changed: - TRUE means that the preference "allow" parameter has been changed - FALSE
	 * means that the preference "allow" paramter has not been changed
	 */
	public boolean hasEnabledValidatorsChanged(ValidatorMetaData[] oldEnabledVmd, boolean allow) throws InvocationTargetException {
		// First check the obvious: is every enabled validator still enabled, and is
		// the number of enabled validators the same as it was before? If not, return true.
		if (super.hasEnabledValidatorsChanged(oldEnabledVmd)) {
			return true;
		}


		// If the global preference validators have changed, does the task list need to be updated?
		// PREF | PROJ | UPDATE
		// ALLOW | OVERRIDE | TASK LIST
		//------------------------------
		//     0 | 0 | 1
		//     0 | 1 | 1
		//     1 | 0 | 1
		//     1 | 1 | 0
		//
		// If the global "allow" preference changes from "allow" to "don't allow", or vice versa,
		// and the project overrides the preferences, and the validators differ between the project
		// and the preferences, then the task list must be updated.
		if (allow) {
			// "allow" has changed, so see if the preference and the project validators match.
			ValidatorMetaData[] projEnabledVmd = super.getEnabledValidators(); // bypass the check
			// for whether the global preferences are to be used or not
			GlobalConfiguration gp = ConfigurationManager.getManager().getGlobalConfiguration();
			return gp.hasEnabledValidatorsChanged(projEnabledVmd);
		}

		return false;
	}

  public boolean haveDelegatesChanged(Map<String, String> oldDelegates, boolean allow) throws InvocationTargetException {

    if (super.haveDelegatesChanged(oldDelegates)) {
      return true;
    }

    if (allow) {
      Map<String, String> projDelegates = super.getDelegatingValidators(); 
      GlobalConfiguration gp = ConfigurationManager.getManager().getGlobalConfiguration();
      return gp.haveDelegatesChanged(projDelegates);
    }
      
    return false;
  }


	public void deserialize(String storedConfiguration) throws InvocationTargetException {
		if (storedConfiguration == null || storedConfiguration.length() == 0 || storedConfiguration.equals(DefaultValue)) {
			resetToDefault();
		} else if (storedConfiguration != null) {
			int prjOverrideIndex = storedConfiguration.indexOf(ConfigurationConstants.PRJ_OVERRIDEGLOBAL);
			int disableAllValidationIndex = storedConfiguration.indexOf(ConfigurationConstants.DISABLE_ALL_VALIDATION_SETTING);
			int versionIndex = storedConfiguration.indexOf(ConfigurationConstants.VERSION);
			if (disableAllValidationIndex != -1) {
				String disableAllValidation = storedConfiguration.substring(disableAllValidationIndex + ConfigurationConstants.DISABLE_ALL_VALIDATION_SETTING.length(), versionIndex);
				setDisableAllValidation(Boolean.valueOf(disableAllValidation).booleanValue());
			} else {
				setDisableAllValidation(false);
			}
			// project doesn't override the global
			if (disableAllValidationIndex != -1) {
				String prjOverride = storedConfiguration.substring(prjOverrideIndex + ConfigurationConstants.PRJ_OVERRIDEGLOBAL.length(), disableAllValidationIndex);
				setDoesProjectOverride(Boolean.valueOf(prjOverride).booleanValue());
			}
		}
	}

	public String serialize() throws InvocationTargetException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(ConfigurationConstants.PRJ_OVERRIDEGLOBAL);
		buffer.append(String.valueOf(doesProjectOverride()));
		if (doesProjectOverride()) {
			// Store common values for the Project configuration only if they differ from the global
			buffer.append(super.serialize());
		}
		return buffer.toString();
	}

	public ValidatorMetaData[] getEnabledFullBuildValidators(boolean fullBuild) throws InvocationTargetException {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getEnabledFullBuildValidators(fullBuild), getResource());
		}
		return super.getEnabledFullBuildValidators(fullBuild);
	}

	/**
	 * Given a set of validators, usually the global preference set, change the set so that it
	 * contains only the validators configured on this project.
	 */
	private static ValidatorMetaData[] extractProjectValidators(ValidatorMetaData[] vmds, IResource resource, boolean onlyReferenced) {
		ValidationRegistryReader reader = ValidationRegistryReader.getReader();
		int length = (vmds == null) ? 0 : vmds.length;
		ValidatorMetaData[] temp = new ValidatorMetaData[length];
		if (length == 0) {
			return temp;
		}

		int count = 0;
		IProject project = (IProject) resource;
		for (int i = 0; i < vmds.length; i++) {
			ValidatorMetaData vmd = vmds[i];
			if (reader.isConfiguredOnProject(vmd, project)) {
				if (!onlyReferenced || vmd.isDependentValidator())
					temp[count++] = vmd;
			}
		}

		ValidatorMetaData[] result = new ValidatorMetaData[count];
		System.arraycopy(temp, 0, result, 0, count);
		temp = null;

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.validation.internal.operations.internal.attribute.ValidationConfiguration#getEnabledFullBuildValidators(boolean)
	 */
	public ValidatorMetaData[] getEnabledFullBuildValidators(boolean fullBuild, boolean onlyReferenced) throws InvocationTargetException {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getEnabledFullBuildValidators(fullBuild), getResource(), onlyReferenced);
		}
		return super.getEnabledFullBuildValidators(fullBuild, onlyReferenced);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.validation.internal.operations.internal.attribute.ValidationConfiguration#getIncrementalValidators()
	 */
	public ValidatorMetaData[] getIncrementalValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return extractProjectValidators(ConfigurationManager.getManager().getGlobalConfiguration().getIncrementalValidators(), getResource());
		}
		return super.getIncrementalValidators();
	}

	public int numberOfManualValidators() throws InvocationTargetException {
		if (useGlobalPreference()) {
			return ConfigurationManager.getManager().getGlobalConfiguration().numberOfManualEnabledValidators();
		}
		return super.numberOfManualEnabledValidators();
	}
	public boolean isDisableAllValidation() throws InvocationTargetException{
		if (useGlobalPreference()) {
			return ConfigurationManager.getManager().getGlobalConfiguration().isDisableAllValidation();
		}
		return super.isDisableAllValidation();
	}	

  public ValidatorDelegateDescriptor getDelegateDescriptor(ValidatorMetaData vmd) throws InvocationTargetException {
    if (useGlobalPreference()) {
      return ConfigurationManager.getManager().getGlobalConfiguration().getDelegateDescriptor(vmd);
    }
    
    return super.getDelegateDescriptor(vmd);
  }
  
  public void store() throws InvocationTargetException {
		IProject project = (IProject) getResource();
		final PreferencesWrapper pref = PreferencesWrapper.getPreferences(project, null);
		if (pref != null) {
			try {
				pref.put(ValidationConfiguration.UserPreference, serialize());
				pref.put(ValidationConfiguration.UserManualPreference, serializeManualSetting());
				pref.put(ValidationConfiguration.UserBuildPreference, serializeBuildSetting());
				pref.put(ValidationConfiguration.DelegatesPreference, serializeDelegatesSetting());
				pref.flush();
			} catch (BackingStoreException e) {
				// A common error is that the project has been closed, in which case there
				// is nothing that we can do.
				if (project.isAccessible())ValidationPlugin.getPlugin().handleException(e);
			}
		}
	}
  
  protected void loadPreference() throws InvocationTargetException {
		IProject project = (IProject) getResource();
		final PreferencesWrapper prefs = PreferencesWrapper.getPreferences(project, null);
		if (prefs.nodeExists()) { 
			String storedConfig = prefs.get(ValidationConfiguration.UserPreference, DefaultValue);
			deserialize(storedConfig);
			String storedManualConfig = prefs.get(ValidationConfiguration.UserManualPreference, DefaultValue);
			deserializeManual(storedManualConfig);
			String storedBuildConfig = prefs.get(ValidationConfiguration.UserBuildPreference, DefaultValue);
			deserializeBuild(storedBuildConfig);
			String storedDelegatesConfiguration = prefs.get(ValidationConfiguration.DelegatesPreference, DefaultValue);
			deserializeDelegates(storedDelegatesConfiguration);
		}
	}
  
  /**
   * Answer the validator id's that have been enabled for manual validation.
   * @return null if they are all enabled
   */
  Set<String> getEnabledManualValidators(){
	  return getIds(ValidationConfiguration.UserManualPreference, ConfigurationConstants.ENABLED_MANUAL_VALIDATORS);
  }
  
  /**
   * Answer the validator id's that have been enabled for build validation.
   * @return null if they are all enabled
   */
  Set<String> getEnabledBuildlValidators(){
	  return getIds(ValidationConfiguration.UserBuildPreference, ConfigurationConstants.ENABLED_BUILD_VALIDATORS);
  }
  
  /**
   * A helper method to extract some validator id's from a preference store field.
   * 
   * @return null if all the validators are enabled.
   */
  private Set<String> getIds(String prefKey, String enabledPhrase){
	IProject project = (IProject) getResource();
	final PreferencesWrapper prefs = PreferencesWrapper.getPreferences(project, null);
	if (prefs == null)return null;
	
	String storedConfig = prefs.get(prefKey, DefaultValue);
	if (storedConfig == null || storedConfig.length() == 0 || storedConfig.equals(DefaultValue))return null;
	int validationIndex = storedConfig.indexOf(enabledPhrase);

	String ids = storedConfig.substring(validationIndex + enabledPhrase.length(),storedConfig.length());

	StringTokenizer tokenizer = new StringTokenizer(ids, ConfigurationConstants.ELEMENT_SEPARATOR);
	Set<String> set = new HashSet<String>(20);
	while (tokenizer.hasMoreTokens())set.add(tokenizer.nextToken());
	if (set.size() == 0)return null;
	  
	return set;
	  
  }
}
