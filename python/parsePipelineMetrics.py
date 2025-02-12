import sys
import json
import subprocess
import argparse
from dataclasses import dataclass
import dataclasses
from datetime import datetime


def main():
    # Run this command before: ada credentials update --account=573868428080 --provider=conduit --role=IibsAdminAccess-DO-NOT-DELETE --once

    args = parse_args()
    pipeline_name = args.pipeline
    pipeline_executions = get_pipeline_executions(pipeline_name)
    pipeline_execution_objs = [process_pipeline_execution(x, pipeline_name, args.start) for x in pipeline_executions]
    output_pipeline_execution(pipeline_execution_objs)

def get_datetime(datetime_str):
    return datetime.strptime(datetime_str.split('.')[0].split('+')[0], '%Y-%m-%dT%H:%M:%S')

def output_pipeline_execution(pipeline_execution_objs):
    file_name = "pipeline_execution.csv"
    print("Output metrics to file: " + file_name)

    with open(file_name, 'w') as f:

        print("ID,Status,Failure count,Duration(minutes),Start time", file=f)
        for obj in pipeline_execution_objs:
           if obj is None:
               continue
           start = get_datetime(obj.startTime)
           end = get_datetime(obj.lastUpdateTime)
           duration = round((end - start).total_seconds() / 60)
           print("{},{},{},{},{}".format(obj.pipelineExecutionId, obj.status, obj.failActionCount, duration, start), file=f)

def process_pipeline_execution(pipeline_execution, pipeline_name, start_time_threshold):
    obj = PipelineExecution(**pipeline_execution)
    start = get_datetime(obj.startTime)
    start_threshold = get_datetime(start_time_threshold)
    if start < start_threshold:
        return None
    action_executions = get_action_executions(pipeline_name, obj.pipelineExecutionId)
    obj.failActionCount = sum(x.status != "Succeeded" for x in action_executions)
    return obj 

def get_pipeline_executions(pipeline_name):
    command = "aws codepipeline list-pipeline-executions --pipeline-name {} --region us-west-2 --output json".format(pipeline_name)
    output = run_command(command)
    pipeline_executions = json.loads(output)["pipelineExecutionSummaries"]
    print("Got {} pipeline executions".format(len(pipeline_executions)))
    return pipeline_executions

def get_action_executions(pipeline_name, pipeline_execution_id):
    command = "aws codepipeline list-action-executions --pipeline-name {} --region us-west-2 --output json --filter pipelineExecutionId={}".format(pipeline_name, pipeline_execution_id)
    output = run_command(command)
    action_executions = json.loads(output)["actionExecutionDetails"]
    print("Got {} action executions".format(len(action_executions)))
    return [ActionExecution(**x) for x in action_executions]

def run_command(command):
    print("run_command: " + command)
    return subprocess.check_output(command.split())

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pipeline")
    parser.add_argument("--start", type=str, default="2024-09-14T07:21:15.748000+00:00")

    return parser.parse_args()



@dataclass
class PipelineExecution:
    pipelineExecutionId: str = None
    status: str = None
    startTime: str = None
    lastUpdateTime: str = None
    failActionCount: int = 0

    def __init__(self, **kwargs):
        names = set([f.name for f in dataclasses.fields(self)])
        for k, v in kwargs.items():
            if k in names:
                setattr(self, k, v)

@dataclass
class ActionExecution:
    status: str = None
    actionName: str = None

    def __init__(self, **kwargs):
        names = set([f.name for f in dataclasses.fields(self)])
        for k, v in kwargs.items():
            if k in names:
                setattr(self, k, v)


main()
