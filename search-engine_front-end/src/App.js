import './App.css';
import React from 'react';
import Home from "./home.js"
import ResultsPage from './ResultsPage';
import { Route } from 'react-router-dom';
import { Routes } from 'react-router-dom';
import { BrowserRouter } from 'react-router-dom';

function App() {
    return (
      <BrowserRouter>
        <Routes>
          <Route index element={<Home />} />  
          <Route path="/:SearchResult" element={<ResultsPage />}>
          </Route>
        </Routes>
      </BrowserRouter>
  );
}

export default App;
