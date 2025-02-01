import os
import pandas as pd
import json

# Define the folder containing JSON files
folder_path = "json_files/"  # Replace with your folder path
output_csv_path = "consolidated.csv"  # Path for the output CSV file

# Initialize a list to store all entries
all_entries = []

# Loop through all files in the folder
for file_name in os.listdir(folder_path):
    if file_name.endswith(".json"):  # Check for JSON files
        file_path = os.path.join(folder_path, file_name)
        
        # Open and parse the JSON file
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                # Ensure each entry has "instruction" and "response"
                for entry in data:
                    if "instruction" in entry and "response" in entry:
                        all_entries.append(entry)
                    else:
                        print(f"Skipping invalid entry in {file_name}")
        except Exception as e:
            print(f"Error processing {file_name}: {e}")

# Convert the entries to a DataFrame
if all_entries:
    df = pd.DataFrame(all_entries)
    # Save the DataFrame to a CSV file
    df.to_csv(output_csv_path, index=False)
    print(f"Consolidated CSV saved to {output_csv_path}")
else:
    print("No valid entries found in the JSON files!")