data_dir=/home/tony/azure-confuzz-data
#rounds=(7253 7290 7291 7292 7293 7294 7295 7301 7302 7311 7312 7313 7314 7315 7316)
#rounds=(7253 7290 7291 7311 7314)
rounds=(7253 7290)
#rounds=(7253)
for rnd in ${rounds[@]}; do
    rm -r outputs$rnd
    for f in $(ls -d $data_dir/r$rnd/*/); do
        echo $f, $rnd
        name=$(echo $f | rev | cut -d/ -f2 | rev | tr "-" "_").zip
        output=$data_dir/exec_zips/$name
        echo $output
        if [[ -f "$output" ]]; then
            echo "$output exists."
            continue
        fi
        mkdir outputs$rnd
        _JAVA_OPTIONS=-Xmx18g mvn confuzz:dumpCov -Ddata.dir=$f -Doutput.dir=outputs$rnd
        zip -r $output outputs$rnd
        rm -rf outputs$rnd
    done
done
wait < <(jobs -p)
