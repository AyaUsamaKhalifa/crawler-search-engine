import React from "react";
import "./ResultItem.css";

function ResultItem(props) {
  const [paragraph, setParagraph] = React.useState("");
  React.useEffect(() => {
    // this part checks that the words that were stemmed are made in bold
    const modifiedParagraph = props.paragraph
      .split(/\b/)
      .map((currWord) => {
        let temp = currWord.toLowerCase();
        if (
          props.searchedWords.some((currentWord) => temp.includes(currentWord))
        ) {
          return `<b>${currWord}</b>`;
        } else {
          return currWord;
        }
      })
      .join("");
    // this part handles the case of phrase searching
    const quotes = props.searchedWords.filter((word) => /["]/.test(word)).map((word) => word.replace(/["]/g, ''));
    const modifiedText = quotes.reduce((Paragraph, currWord) => {
      return Paragraph.replace(new RegExp(currWord, "gi"), `<b>${currWord}</b>`);
    }, modifiedParagraph);
    // this parts sets the paragraph
    setParagraph(modifiedText);

  }, [props.paragraph, props.searchedWords]);
  return (
    <div className="result-item">
      <h2>{props.title}</h2>
      <a href={props.link}>{props.link}</a>
      <p dangerouslySetInnerHTML={{ __html: paragraph }} />
    </div>
  );
}

export default ResultItem;
