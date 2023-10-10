TARGET_DIR=$1
files=$(find $TARGET_DIR -type f)
for key in ${files[@]}; do
  #echo $key
  sed -i "s/@;@/¬/g" $key
done
