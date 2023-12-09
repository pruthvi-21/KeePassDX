import os

folder_path = "/Users/pruthvi/AndroidStudioProjects/KeePass/icon-pack/keepassxc/src/main/res/drawable"

# Check if the folder path exists
if os.path.exists(folder_path):
    # Iterate over the files in the folder
    for filename in os.listdir(folder_path):
        # Check if the filename starts with "material_"
        if filename.startswith("material_"):
            # Create the new filename by replacing "material_" with "keepassxc_"
            new_filename = filename.replace("material_", "keepassxc_")
            
            # Construct the full paths for the old and new filenames
            old_filepath = os.path.join(folder_path, filename)
            new_filepath = os.path.join(folder_path, new_filename)
            
            # Rename the file
            os.rename(old_filepath, new_filepath)
            
            print(f'Renamed: {filename} to {new_filename}')
else:
    print(f"The specified folder path '{folder_path}' does not exist.")
