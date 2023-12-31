# bblf 

BaBashka Labmda Function

Utilities for building and working with babashka scripts as lambda functions

## todods 

- [x] opts for config stuff
- [x] redo the bootstrap/tasks/etc maybe package as bbin?
- [ ] looks at the task config stuff [here](https://github.com/babashka/babashka/wiki/Self-contained-executable)
- [ ] make the library bits for working with lambda includable
- [ ] figure out what to do with LICENSE and Credits
- [ ] use self contained executable for packaging [see](https://github.com/babashka/babashka/wiki/Self-contained-executable#uberjar)

ideal state: I'd like to be able to have a pretty normal function and wrap it in handler 
bits for lambda and call a task to make the lambda zip artifact

## usage

There are 2 distinct modes of operation:

1. CLI for packaging lambda functions. 

*update code snippet to use published version vs. local*

    ```sh
    bbin install . 
    bblf build
    ```

2. Library for wrapping existing fuctions for use as lambda fuctions.

```edn
normal deps icclued, require and wrap stuff
```

somthing like: 

```clj
(require '[em-schmidt/bblf :as lf])

(defn dostuff
    []
    (println "I did stuff"))

(defn entrypoint
    []
    (lf/run dostuff))
```


## Credits

This started as a fork of blabmda, but I quickly ran into issues that I needed to resolve
with regards to pod depenedncies, deployment style, etc. 

Much credit to prior art:

[blambda](https://github.com/jmglov/blambda)
[bb-lambda](https://github.com/tatut/bb-lambda)
[babashka-lambda](https://github.com/dainiusjocas/babashka-lambda)

