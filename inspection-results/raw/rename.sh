(IFS='
'
for fn in $(ls *.tsv); do
    n=$(echo $fn | cut -d- -f2 | cut -d" " -f1).tsv
    mv $fn r6284/$n
done)
