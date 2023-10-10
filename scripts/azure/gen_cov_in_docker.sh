filter=$1

gen_csv () {
    tag=$1
    mvn confuzz:dumpCov -Ddata.dir=$tag.txt -Doutput.dir=$tag-exec
    mkdir $tag-output
    for line in $(ls ${tag}-exec | grep exec | sort -n); do
        ts=$(echo $line|cut -d. -f1)
        java -jar jacococli.jar report ${tag}-exec/$line $(cat cp.txt | tr ":" "\n" | grep "org/apache/alluxio/common" | grep -v "thirdparty"| sed "s/^/--classfiles /g" | paste -sd " " -) --classfiles target --csv $tag-output/$ts.csv
    done
}

mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
gen_csv Confuzz
gen_csv JQF
gen_csv Random

