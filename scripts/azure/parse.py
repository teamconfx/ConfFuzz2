from pathlib import Path
import config, sys, json
import parse_summary, parse_failure_csv, remove_duplicated, get_exceptions


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python parse_all.py <result_dir>")
        exit(-1)

    result_dir = Path(sys.argv[1])
    parse_summary.main(result_dir)
    failures = parse_failure_csv.main(result_dir)
    project = [key for key, value in config.PROJECTS_MAPPING.items() if value in str(result_dir)][0]
    clusters = remove_duplicated.main(failures, result_dir / "unique_failures.csv", project=project)
    get_exceptions.main(result_dir / "unique_failures.csv", result_dir / "exception_summary.csv")
    with open(result_dir / "unique_failures.json", "w") as f:
        json.dump(clusters, f)
