import json, re, io, csv, sys
from zipfile import ZipFile
from pathlib import Path
from util import match_strings
from typing import Dict, List, Tuple, TypeVar
from enum import StrEnum
from config import DUMP_LIMIT, PREV_COMPRESSED_RESULT_DIR, NEW_DELIMITER, BUG_FILE
class ReproStatus(StrEnum):
    REPRODUCIBLE = "REPRODUCIBLE"
    DIFFERENT = "DIFFERENT"
    PASS = "PASS"
    FLAKY = "FLAKY"
    POLLUTED = "POLLUTED"

class Throwable():
    clazz: str
    message: str
    Trace: List[str]
    testClass: str
    def __init__(self, items: List, testClass:str):
        assert len(items) == 3, items
        self.testClass = testClass
        self.clazz, self.message, self.stackTrace = items

    def __eq__(self, other: 'Throwable'):
        return hash(self) == hash(other)
    
    def isFailed(self) -> bool:
        return self.clazz == "" and self.message == "" and self.stackTrace == []

    def hash(self) -> str:
        THRESH=1
        def get_test_class(trace: str) -> str:
            test = '.'.join(trace.split("(")[0].split(".")[:-1])
            return test
        bound = len(self.stackTrace)
        while bound > 0:
            trace = self.stackTrace[bound - 1]
            if get_test_class(trace) == self.testClass and not "CONFUZZ" in trace:
                break
            bound -= 1

        if bound == 0:
            bound = len(self.stackTrace)
        else:
            bound -= 1

        trimmedStackTrace = [trace for trace in self.stackTrace[:bound]
                             if "jdk.internal.reflect" not in trace and "junit" not in trace][:THRESH]
        return '\n'.join(trimmedStackTrace)

    def __hash__(self) -> int:
        return hash(self.hash())

    def dump(self) -> List[str]:
        return [self.clazz, self.message, ', '.join(self.stackTrace)]

class Failure():
    testClass: str
    testMethod: str
    failure: Throwable
    reproStatus: str
    replayedFailure: Throwable
    replayedFile: str
    minConfig: Dict[str, str]
    debugFiles: List[str]
    def __init__(self, items: Dict|List|'Failure'):
        if type(items) == dict:
            self.testClass = items["testClass"]
            self.testMethod = items["testMethod"]
            self.failure = Throwable([items["failure"], items["errorMessage"], items["stackTrace"]], self.testClass)
            self.reproStatus = items["reproStatus"]
            if self.reproStatus == "Non-Reproducible":
                print(f"{self.testClass} Non-Reproducible")
            
            self.replayedFailure = Throwable([items.get("replayedFailure", ""),
                                              items.get("replayedErrorMessage", ""),
                                              items.get("replayedStackTrace", [])], self.testClass)
            self.replayedFile = items.get('replayedFile', "")
            self.minConfig = items["minConfig"]
            self.debugFiles = items.get("debugFiles", [])
        elif type(items) == list:
            self.testClass = items[0].split("#")[0]
            self.testMethod = items[0].split("#")[1]
            self.failure = Throwable(items[1:3] + [items[3].split(',')], self.testClass)
            self.reproStatus = items[4]
            self.replayedFailure = Throwable(items[5:7] + [items[7].split(',')], self.testClass)
            self.replayedFile = items[8]
            self.minConfig = json.loads(items[9])
            self.debugFiles = json.loads(items[10])
        elif type(items) == 'Failure':
            for k,v in items.__dict__.items():
                self.__dict__[k] = v

    def __eq__(self, other: 'Failure'):
        return hash(self) == hash(other)

    def equals(self, other: 'Failure'):
        """check if two failures are EXACTLY the same"""
        return self.testClass == other.testClass \
                and self.testMethod == other.testMethod \
                and self.failure.clazz == other.failure.clazz \
                and self.failure.message == other.failure.message \
                and self.failure.stackTrace == other.failure.stackTrace \
                and self.minConfig == other.minConfig

    def testName(self):
        return self.testClass + "#" + self.testMethod

    def __hash__(self) -> int:
        throwable = self.failure
        if self.reproStatus == ReproStatus.DIFFERENT.value:
            throwable = self.replayedFailure
        return hash((self.failure.clazz, self.reproStatus, tuple(sorted(list(self.minConfig.keys()))), throwable))

    def auto_debug(self):
        # is this an FP?
        # be sure to consider also DIFFERENT
        if self.reproStatus in [ReproStatus.REPRODUCIBLE.value, ReproStatus.DIFFERENT.value]:
            throwable = None
            if self.reproStatus == ReproStatus.REPRODUCIBLE.value:
                throwable = self.failure
            elif self.reproStatus == ReproStatus.DIFFERENT.value:
                throwable = self.replayedFailure
            assert throwable is not None

            if "Assertion" in throwable.clazz or throwable.clazz == "org.junit.ComparisonFailure":
                matches = re.findall(r"<(.*?)>", throwable.message)
                if any(any(match_strings(match, val) for match in matches) for val in self.minConfig.values()):
                    return "Filtered"
            elif "IllegalArgument" in throwable.clazz:
                if any(val in throwable.message.split(" ") for val in self.minConfig.values()):
                    return "Filtered"
            elif "IOException" in throwable.clazz and "Unexpected configuration" in throwable.clazz:
                if any(val in throwable.message.split(" ") for val in self.minConfig.values()):
                    return "Filtered"

        # is this a BUG?
        if self.reproStatus == ReproStatus.REPRODUCIBLE.value:
            if self.failure.clazz == "java.lang.NegativeArraySizeException":
                if self.failure.message in self.minConfig.values():
                    return "BUG"
            # TODO: add NPE, OOB, and Division by 0 here

        return ""

    def dump(self) -> List[str]:
        return [self.testName()] + self.failure.dump() + [self.reproStatus] + self.replayedFailure.dump() +\
                [self.replayedFile, json.dumps(self.minConfig), json.dumps(self.debugFiles)]

# Inspected failure with bugId and label
class PreviousFailure(Failure):
    status: str = ""
    bugId: list = []
    label: str = ""

    def __init__(self, items: Dict|None = None, failure: Failure|None = None, label: str = ""):
        self.label = label
        if items != None:
            super().__init__(items)
            self.status = items["status"]
            self.bugId = items["bugId"]
        elif failure is not None:
            super().__init__(failure)

    def __hash__(self):
        return super().__hash__()

    def __eq__(self, other):
        return super().__eq__(other)

    def naiveStatus(self, bugOnly:bool=False) -> str:
        if self.status in ["Filtered", "FP", "CONFUZZ"]:
            return "FP"
        elif self.status in ["BUG", "Repeated", "Repeated-BUG", "NO-MSG", "TEST-BUG"]:
            if bugOnly:
                return "BUG"
            return ','.join(sorted(self.bugId))
        return self.status

    def dump(self):
        print("attempting to dump a PreviousFailure!")
        return super().dump()

    def equals(self, other: 'Failure'):
        return super().equals(other)

UniqueFailureType = TypeVar('UniqueFailureType', bound='UniqueFailure')
class UniqueFailure():
    failures: List[Failure|PreviousFailure]
    status: str
    bugId: list
    label: str
    def __init__(self, failures: List[Failure], autoDebug:bool = False, considerNotReproducible:bool = False):
        self.failures = failures
        if autoDebug:
            self.auto_debug(considerNotReproducible)

    def __hash__(self) -> int:
        assert len(self.failures) > 0
        return hash(self.failures[0])

    def __eq__(self, other: UniqueFailureType | Failure) -> bool:
        assert issubclass(type(other), Failure) or issubclass(type(other), UniqueFailure)
        return hash(self) == hash(other)

    def auto_debug(self, considerNotReproducible:bool = False):
        self.status, previousFailure = UniqueFailure._auto_debug(self.failures, considerNotReproducible)
        self.failures = [f for f in self.failures if type(f) != PreviousFailure]
        self.bugId = []
        # we want to put the debugged one at the first
        if previousFailure != None:
            self.bugId = previousFailure.bugId
            for idx, failure in enumerate(self.failures):
                if failure.equals(previousFailure):
                    self.failures = [failure] + self.failures[:idx] + self.failures[idx+1:]
                    break

    def naiveStatus(self) -> str:
        if self.status in ["Filtered", "FP"]:
            return "FP"
        elif self.status == "Repeated":
            return ','.join(sorted(self.bugId))
        elif self.status == "BUG":
            return "BUG"
        else:
            return ""

    @staticmethod
    def _auto_debug(failures: List[Failure], considerNotReproducible:bool = False) -> Tuple[str, PreviousFailure|None]:
        # first try to find a hint from previous failures
        try:
            #assert len({x.naiveStatus() for x in failures if type(x) == PreviousFailure}) <= 1, \
            #        ({x.naiveStatus() for x in failures if type(x) == PreviousFailure}, [x.dump() for x in failures if type(x) == PreviousFailure])
            previousFailure: PreviousFailure = next(x for x in failures if type(x) == PreviousFailure
                                                    and (considerNotReproducible or x.status != "Non-Reproducible"))
            status = "Repeated" if previousFailure.status not in ["FP", "CONFUZZ", "Non-Reproducible", "Filtered"] else previousFailure.status
            return status, previousFailure
        except StopIteration:
            pass

        try:
            status: str = next(y for y in map(lambda x: x.auto_debug(), failures) if y != "")
            return status, None
        except StopIteration:
            pass

        return "", None

    def dump(self) -> List[str]:
        return [','.join(sorted(self.bugId)), self.status, str(len(self.failures))] + \
            sum(map(lambda x: x.dump(), self.failures[:DUMP_LIMIT]), [])

# failures generated by fuzz-fp goal
class FuzzedFailure():
    testClass: str
    testMethod: str
    failure: Throwable
    config: Dict[str, str]
    def __init__(self, items: Dict, testClass, testMethod):
        self.testClass = testClass
        self.testMethod = testMethod
        self.failure = Throwable([items.get("failure", ""), items.get("errorMessage", ""), items.get("stackTrace", [])], self.testClass)
        self.config = items.get("config", {})
    
    
    def __eq__(self, other: 'PreviousFailure') -> bool:
        return hash(self) == hash(other)
    def isFailure(self, other) -> bool:
        return self.failure.isFailed()
    def partialEq(self, other: "PreviousFailure") -> bool:
        return self.failure.clazz == other.failure.clazz

    metrics = {
            'failureOnly': isFailure,
            'failureAndType': partialEq,
            'equiv': __eq__
            }

    def equals(self, other: 'PreviousFailure'):
        """check if two failures are EXACTLY the same"""
        return self.testClass == other.testClass \
                and self.testMethod == other.testMethod \
                and self.failure.clazz == other.failure.clazz \
                and self.failure.message == other.failure.message \
                and self.failure.stackTrace == other.failure.stackTrace \
                and self.config == other.minConfig

    def testName(self):
        return self.testClass + "#" + self.testMethod

    def isFailed(self) -> bool:
        return self.failure.clazz == ""

    def __hash__(self) -> int:
        return hash((self.failure.clazz, "REPRODUCIBLE", 
                     tuple(sorted(list(self.config.keys()))), self.failure))

# raw data related infrastructure classes
class ProjectRound:
    rndId: int
    project: str
    VMs: List['VM']

# Azure VM - per zip file
class VM:
    idx: int
    module: str
    zipFilePath: Path
    zipFile: ZipFile
    tests: List[str]|None
    def __init__(self, zipFile: Path, module: str):
        self.idx = int(zipFile.name.split('_')[-2])
        self.module = module
        self.zipFilePath = zipFile
        self.zipFile = ZipFile(zipFile)
        self.tests = None

    @staticmethod
    def getVMs(resultDir: Path, module: str):
        return [VM(vm, module) for vm in resultDir.glob("*.zip")]

    def getCampaignForTest(self, test: str):
        assert test in self.getTests()
        #print(f"Test: {test}, zipFile: {self.zipFile}")
        return Campaign(self.idx, self.zipFile, test, self.module)

    def getTests(self):
        if self.tests is None:
            with self.zipFile.open(f"{self.module}_input_{self.idx}/{self.module}_input_{self.idx}.csv") as f:
                line = f.readline().decode('utf-8')
            self.tests = line.split(',')[-2].split('+')
        return self.tests

class Campaign:
    """A Meringue Campaign"""
    zipFile: ZipFile
    test: str
    campaignPath: Path
    logPath: Path
    def __init__(self, idx: int, zipFile: ZipFile, test: str, module: str):
        self.zipFile = zipFile
        self.test = test
        self.campaignPath = Path(f"{module}_input_{idx}/output/result/{test.replace('#', '/')}")
        self.logPath = Path(f"{module}_input_{idx}/{module}_input_{idx}.out")

    def exists(self) -> bool:
        # there is an extra '/' at the end of the path returned by namelist()
        return str(self.campaignPath) + "/" in self.zipFile.namelist()

    def getFuzzTimeFromLog(self):
        if str(self.logPath) not in self.zipFile.namelist():
            print(f"{logPath} not found")
            return None
        test_class = self.test.split('#')[0]
        test_method = self.test.split('#')[1]
        startLine = f"[INFO] Running fuzzing campaign: {self.test}"
        no_param_recorded = False
        no_enough_memory = False
        no_enough_space = False
        confuzz_bug = False
        # search for the startLine in the self.logPath, and get the line that contains with "Total time" before the endLine
        with self.zipFile.open(str(self.logPath)) as f:
            startFlag = False
            for line in f:
                line = line.decode('utf-8')
                if startLine in line:
                    startFlag = True
                if startFlag:
                    if "No configuration parameter tracked from test" in line:
                        no_param_recorded = True
                    if "insufficient memory for the Java Runtime Environment " in line:
                        no_enough_memory = True
                    if "Not enough space" in line:
                        no_enough_space = True
                    if "ConfuzzGuidance" in line:
                        confuzz_bug = True
                    
                    if "[INFO] Total time: " in line:
                        timeStr = line.split()[-2]
                        if "min" in line:
                            return timeStr, no_param_recorded, no_enough_memory, no_enough_space, confuzz_bug
                        elif "s" in line:
                            return "00" + ":" + timeStr.split('.')[0], no_param_recorded, no_enough_memory, no_enough_space, confuzz_bug
                        print(f"Unknown time format: {line}")
                        return None, no_param_recorded, no_enough_memory, no_enough_space, confuzz_bug
        
    def getFuzzTime(self) -> int|None:
        fuzzTimePath = self.campaignPath / "fuzz.time"
        if str(fuzzTimePath) not in self.zipFile.namelist():
            # Return None and script should try to use getFuzzTimeFromLog() to get the time
            return None

        fuzzTimePattern:re.Pattern = re.compile(r"Time: (\d+)ms")
        with self.zipFile.open(str(fuzzTimePath)) as f:
            content = f.read().decode('utf-8')
        match = fuzzTimePattern.match(content)
        assert match != None
        return int(match.group(1))
    
    def getCoverageTrend(self) -> List[Tuple[int, int]] | None:
        fuzzTimePath = self.campaignPath / "coverage_trend.csv"
        if str(fuzzTimePath) not in self.zipFile.namelist():
            return None
        with self.zipFile.open(str(fuzzTimePath)) as f:
            reader = csv.DictReader(io.TextIOWrapper(f, 'utf-8'))
            return [(int(line["time"]), int(line['covered_branches'])) for line in reader]

    def getCorpusSize(self) -> int|None:
        if f'{self.campaignPath / "campaign/corpus"}/' not in self.zipFile.namelist():
            return None

        idPaths = [x for x in self.zipFile.namelist() if x.startswith(f'{self.campaignPath / "campaign/corpus"}/id')]
        return len(idPaths)

    def getFailureSize(self) -> int|None:
        if f'{self.campaignPath / "campaign/failures"}/' not in self.zipFile.namelist():
            return None

        idPaths = [x for x in self.zipFile.namelist() if x.startswith(f'{self.campaignPath / "campaign/failures"}/id')]
        return len(idPaths)

####################################################################################################################################
# Auxiliary functions
####################################################################################################################################

def getInspectedFailures(project: str|None) -> List[PreviousFailure]:
    if project is None:
        return []
    project = project.lower()
    ret: List[PreviousFailure] = []
    for round_dir in filter(lambda x: x.is_dir(), PREV_COMPRESSED_RESULT_DIR.iterdir()):
        if (round_dir / f"{project}.json").exists():
            with open(round_dir / f"{project}.json") as json_file:
                ret += [PreviousFailure(failure, label=round_dir.name) for failure in json.load(json_file)]
    return ret

def dumpUniqueFailuresTo(path: Path, clusters: List[UniqueFailure]) -> None:
    assert path != None
    fields = ["testName", "failure", "failureMessage", "stackTrace", "reproStatus", "replayedFailure",
              "replayedErrorMessage", "replayedStackTrace", "replayedFile", "minConfig", "debugFiles"]
    with open(path, 'w') as f:
        f.write(NEW_DELIMITER.join(["note", "bugId", "status", "times"] + fields) + "\n")
        for uniqueFailure in clusters:
            f.write(NEW_DELIMITER.join(uniqueFailure.dump()) + "\n")

def getFailuresFromFile(input_file: Path) -> List[Failure]:
    with open(input_file) as f:
        next(f)
        return [Failure(line.strip().split(NEW_DELIMITER)) for line in f.readlines()]

def getBugs() -> Dict[str, Dict[str, str]]:
    with open(BUG_FILE) as f:
        bugs = json.load(f)
    bugMap = {bug['bugId']: bug for bug in bugs}
    return bugMap

def isTrivial(bug: Dict[str, str]) -> bool:
    return bug['type'] not in ['OOM', 'No Checker / Missing(Wrong) Message', 'Test Bug']
