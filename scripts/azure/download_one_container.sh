containername=$1
outputdir=$2
AZCOPY_AUTO_LOGIN_TYPE=DEVICE ./azcopy copy https://confuzzstorage.blob.core.windows.net/${containername}/ ${outputdir} --recursive
