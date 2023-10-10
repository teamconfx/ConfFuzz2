from azure.storage.blob import BlobServiceClient
import sys, os
storage_connection_string = "BlobEndpoint=https://confuzzstorage.blob.core.windows.net/;QueueEndpoint=https://confuzzstorage.queue.core.windows.net/;FileEndpoint=https://confuzzstorage.file.core.windows.net/;TableEndpoint=https://confuzzstorage.table.core.windows.net/;SharedAccessSignature=sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2024-08-31T09:55:52Z&st=2023-06-29T01:55:52Z&spr=https&sig=fpLR0lady04WJXU4BTfVUAN8WgZO6TZNWPn8AlXGxZw%3D"

def connect_to_azure_storage(storage_connection_string):
    blob_service_client = BlobServiceClient.from_connection_string(storage_connection_string)
    return blob_service_client


def get_container_names(blob_service_client, round):
    PREFIX = "r{}-".format(round)
    SUFFIX = "-output"
    container_names = []
    for container in blob_service_client.list_containers():
        if container.name.startswith(PREFIX) and container.name.endswith(SUFFIX):
            container_names.append(container.name)
    return container_names


def download_container(blob_service_client, container_name, destination_folder):
    container_client = blob_service_client.get_container_client(container_name)

    # Retrieve all blobs in the container
    blob_list = container_client.list_blobs()
    # create destination folder if not exist
    if not os.path.exists(destination_folder):
        os.makedirs(destination_folder)
    for blob in blob_list:
        blob_client = container_client.get_blob_client(blob.name)
        destination_file_path = f"{destination_folder}/{blob.name}"

        with open(destination_file_path, "wb") as file:
            data = blob_client.download_blob()
            data.readinto(file)

        print(f"Blob '{blob.name}' downloaded to '{destination_folder}'.")


def download_containers(storage_connection_string, round, destination_folder):
    blob_service_client = connect_to_azure_storage(storage_connection_string)
    destination_folder = os.path.join(destination_folder, f"r{round}")
    container_names = get_container_names(blob_service_client, round)
    for container_name in container_names:
        proj_name = container_name.replace("-output", "")
        sub_destination_folder = f"{destination_folder}/{proj_name}/{container_name}"
        download_container(blob_service_client, container_name, sub_destination_folder)


# Call the function to download the containers
if __name__ == '__main__':
    if (len(sys.argv) != 3):
        raise ValueError("Usage: python3 download_containers.py <round> <destination_folder>")
    round, destination_folder = sys.argv[1:]
    download_containers(storage_connection_string, round, destination_folder)
