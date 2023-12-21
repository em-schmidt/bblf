# bblf 

BaBashka Labmda Function

Utilities for building and working with babashka scripts as lambda functions

## usage

```sh
bbin install .
bblf
```

## todods 

- [ ] redo the bootstrap/tasks/etc maybe package as bbin?
- [ ] make the library bits for working with lambda includable
- [ ] figure out what to do with LICENSE and Credits

ideal state: I'd like to be able to have a pretty normal function and wrap it in handler 
bits for lambda and call a task to make the lambda zip artifact

somthing like: 

```clj
(reqire '[em-schmidt/this :as lambda])

(defn dostuff
    []
    (println "I did stuff"))

(defn entrypoint
    []
    (lambda/run dostuff))
```

## Credits

This started as a fork of blabmda, but I quickly ran into issues that I needed to resolve
with regards to pod depenedncies, deployment style, etc. 

Much credit to prior art:

[blambda](https://github.com/jmglov/blambda)
[bb-lambda](https://github.com/tatut/bb-lambda)
[babashka-lambda](https://github.com/dainiusjocas/babashka-lambda)
